package com.github.bobi.aemgroovyconsoleplugin.services

import com.github.bobi.aemgroovyconsoleplugin.services.model.AemServerConfig
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "AEMServers")
class PersistentStateService : PersistentStateComponent<PersistentStateService.State> {
    private var myState: State = State()

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        myState = state
    }

    fun getAEMServers(): List<AemServerConfig> {
        return myState.aemServers
    }

    fun setAEMServers(servers: List<AemServerConfig>) {
        myState = State(servers.toMutableList())
    }

    fun findById(id: Long): AemServerConfig? = getAEMServers().find { it.id == id }

    data class State(var aemServers: MutableList<AemServerConfig> = mutableListOf())

    companion object {
        @JvmStatic
        fun getInstance(project: Project): PersistentStateService = project.service()
    }
}
