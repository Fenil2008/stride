package com.example.ui.navigation

sealed class Screen(val route: String, val label: String) {
    object Feed : Screen("feed", "Feed")
    object Explore : Screen("explore", "Explore")
    object Record : Screen("record", "Record")
    object History : Screen("history", "History")
    object Profile : Screen("profile", "Profile")
    object Auth : Screen("auth", "Auth")
    object ProfileSetup : Screen("profile_setup", "Profile Setup")
    object PostActivity : Screen("post_activity", "Summary")
    object ActivityDetail : Screen("activity_detail/{activityId}", "Activity") {
        fun createRoute(activityId: String) = "activity_detail/$activityId"
    }
}
