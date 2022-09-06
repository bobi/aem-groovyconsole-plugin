package com.github.bobi.aemgroovyconsoleplugin.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@State(name = "AemScriptRoots")
class RootFoldersService : PersistentStateComponent<RootFoldersService.State> {
    private var myState: State = State()

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        myState = state
    }

    fun addRoot(path: String) {
        if (!myState.roots.contains(path)) {
            myState.roots.add(path)
        }
    }

    fun removeRoot(path: String) {
        if (myState.roots.contains(path)) {
            myState.roots.remove(path)
        }
    }

    fun hasRoot(path: String) = myState.roots.contains(path)

    fun isInsideAnyRoot(path: String): Boolean {
        return myState.roots.any { it != path && path.startsWith("${it}/") }
    }

    data class State(var roots: MutableSet<String> = mutableSetOf())

    companion object {
        @JvmStatic
        fun getInstance(project: Project): RootFoldersService = project.service()
    }
}