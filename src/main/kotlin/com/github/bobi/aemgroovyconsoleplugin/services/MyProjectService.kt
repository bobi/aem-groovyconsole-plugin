package com.github.bobi.aemgroovyconsoleplugin.services

import com.intellij.openapi.project.Project
import com.github.bobi.aemgroovyconsoleplugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
