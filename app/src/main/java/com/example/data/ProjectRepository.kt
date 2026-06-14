package com.example.data

import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val dao: ProjectDao) {
    val allProjects: Flow<List<ProjectEntity>> = dao.getAllProjects()
    val allExports: Flow<List<ExportEntity>> = dao.getAllExports()

    suspend fun getProjectById(id: Int): ProjectEntity? = dao.getProjectById(id)
    suspend fun insertProject(project: ProjectEntity): Long = dao.insertProject(project)
    suspend fun updateProject(project: ProjectEntity) = dao.updateProject(project)
    suspend fun deleteProject(project: ProjectEntity) = dao.deleteProject(project)
    suspend fun deleteProjectById(id: Int) = dao.deleteProjectById(id)

    suspend fun insertExport(export: ExportEntity): Long = dao.insertExport(export)
    suspend fun deleteExportById(id: Int) = dao.deleteExportById(id)
}
