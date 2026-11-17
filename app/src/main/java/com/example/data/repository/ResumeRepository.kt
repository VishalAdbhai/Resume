package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.PdfPageSize
import com.example.data.model.ResumeAccent
import com.example.data.model.ResumeProfile
import com.example.data.model.ResumeTemplate
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ResumeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireAuthorizedUser(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user access." }
        return uid
    }

    fun observeResumes(userId: String): Flow<List<ResumeProfile>> =
        db.collection("users").document(userId).collection("resumes")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot
                    .toObjects(
                        ResumeProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeResume(userId: String, resumeId: String): Flow<ResumeProfile?> =
        db.collection("users").document(userId).collection("resumes").document(resumeId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    ResumeProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun createResume(userId: String, resume: ResumeProfile): Result<String> {
        val colRef = db.collection("users").document(userId).collection("resumes")
        var docPath = "users/$userId/resumes"
        return try {
            val uid = requireAuthorizedUser(userId)
            val normalized = normalizeForRules(resume)
            val docRef = colRef.document()
            docPath = docRef.path
            docRef.set(toCreateMap(uid, normalized)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateResume(userId: String, resumeId: String, resume: ResumeProfile): Result<Unit> {
        val docPath = "users/$userId/resumes/$resumeId"
        return try {
            requireAuthorizedUser(userId)
            require(resumeId.isNotBlank()) { "Resume ID must not be blank for update" }
            val normalized = normalizeForRules(resume)
            val docRef = db.collection("users").document(userId).collection("resumes").document(resumeId)
            docRef.update(toUpdateMap(normalized)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun duplicateResume(userId: String, source: ResumeProfile, newTitle: String): Result<String> {
        val copy = source.copy(
            id = "",
            userId = userId,
            title = newTitle.trim().ifBlank { "${source.title} (Copy)" }.take(100),
            createdAt = null,
            updatedAt = null
        )
        return createResume(userId, copy)
    }

    suspend fun deleteResume(userId: String, resumeId: String): Result<Unit> {
        val docPath = "users/$userId/resumes/$resumeId"
        return try {
            requireAuthorizedUser(userId)
            require(resumeId.isNotBlank()) { "Resume ID must not be blank for delete" }
            db.collection("users").document(userId).collection("resumes").document(resumeId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun normalizeForRules(resume: ResumeProfile): ResumeProfile {
        val cleanTitle = resume.title.trim().ifBlank { "Untitled Resume" }.take(100)
        return resume.copy(
            title = cleanTitle,
            templateId = ResumeTemplate.fromRaw(resume.templateId).name,
            accentId = ResumeAccent.fromRaw(resume.accentId).name,
            pageSize = PdfPageSize.fromRaw(resume.pageSize).name,
            fullName = resume.fullName.trim().take(120),
            headline = resume.headline.trim().take(150),
            email = resume.email.trim().take(150),
            phone = resume.phone.trim().take(50),
            location = resume.location.trim().take(120),
            website = resume.website.trim().take(250),
            linkedIn = resume.linkedIn.trim().take(250),
            summary = resume.summary.trim().take(2000),
            experiences = resume.experiences.take(30),
            educations = resume.educations.take(20),
            skills = resume.skills.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(50),
            projects = resume.projects.take(30)
        )
    }

    private fun toCreateMap(uid: String, resume: ResumeProfile): Map<String, Any> = mapOf(
        "userId" to uid,
        "title" to resume.title,
        "templateId" to resume.templateId,
        "accentId" to resume.accentId,
        "pageSize" to resume.pageSize,
        "showPhotoInPdf" to resume.showPhotoInPdf,
        "fullName" to resume.fullName,
        "headline" to resume.headline,
        "email" to resume.email,
        "phone" to resume.phone,
        "location" to resume.location,
        "website" to resume.website,
        "linkedIn" to resume.linkedIn,
        "summary" to resume.summary,
        "experiences" to resume.experiences.map { it.toFirestoreMap() },
        "educations" to resume.educations.map { it.toFirestoreMap() },
        "skills" to resume.skills,
        "projects" to resume.projects.map { it.toFirestoreMap() },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun toUpdateMap(resume: ResumeProfile): Map<String, Any> = mapOf(
        "title" to resume.title,
        "templateId" to resume.templateId,
        "accentId" to resume.accentId,
        "pageSize" to resume.pageSize,
        "showPhotoInPdf" to resume.showPhotoInPdf,
        "fullName" to resume.fullName,
        "headline" to resume.headline,
        "email" to resume.email,
        "phone" to resume.phone,
        "location" to resume.location,
        "website" to resume.website,
        "linkedIn" to resume.linkedIn,
        "summary" to resume.summary,
        "experiences" to resume.experiences.map { it.toFirestoreMap() },
        "educations" to resume.educations.map { it.toFirestoreMap() },
        "skills" to resume.skills,
        "projects" to resume.projects.map { it.toFirestoreMap() },
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
