package com.appclonex.infrastructure.logging

import com.appclonex.domain.usecase.monitoring.UserEventTracker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserEventTrackerImpl @Inject constructor(
    private val analyticsEventTracker: FirebaseAnalyticsEventTracker
) : UserEventTracker {
    override fun trackCloneApp(packageName: String, profileId: Int, success: Boolean) =
        analyticsEventTracker.trackCloneApp(packageName, profileId, success)

    override fun trackDeleteApp(packageName: String, profileId: Int) =
        analyticsEventTracker.trackDeleteApp(packageName, profileId)

    override fun trackLaunchApp(packageName: String) =
        analyticsEventTracker.trackLaunchApp(packageName)

    override fun trackFreezeApp(packageName: String, frozen: Boolean) =
        analyticsEventTracker.trackFreezeApp(packageName, frozen)

    override fun trackCreateProfile(profileName: String) =
        analyticsEventTracker.trackCreateProfile(profileName)

    override fun trackDeleteProfile(profileId: Int) =
        analyticsEventTracker.trackDeleteProfile(profileId)

    override fun trackShizukuPermission(granted: Boolean) =
        analyticsEventTracker.trackShizukuPermission(granted)

    override fun trackScreenView(screenName: String) =
        analyticsEventTracker.trackScreenView(screenName)
}
