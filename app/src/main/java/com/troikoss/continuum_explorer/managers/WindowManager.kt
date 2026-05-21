package com.troikoss.continuum_explorer.managers

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object WindowManager {
    private val _restrictedRightPadding = mutableStateOf(0.dp)
    val restrictedRightPadding: State<Dp> = _restrictedRightPadding

    private val _restrictedLeftPadding = mutableStateOf(0.dp)
    val restrictedLeftPadding: State<Dp> = _restrictedLeftPadding

    fun updateRestrictedArea(left: Dp, right: Dp) {
        _restrictedLeftPadding.value = left
        _restrictedRightPadding.value = right
    }
}
