package com.example.homeserve.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun AdminRootLayout(
    currentRoute: AdminRoute,
    onRouteSelected: (AdminRoute) -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        content(Modifier.weight(1f))
        AdminBottomNavigation(
            currentRoute = currentRoute,
            onRouteSelected = onRouteSelected
        )
    }
}
