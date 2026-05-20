package com.appclonex.domain.usecase.monitoring

import javax.inject.Inject

/**
 * 用户事件追踪用例
 */
interface UserEventTracker {
    fun trackCloneApp(packageName: String, profileId: Int, success: Boolean)
    fun trackDeleteApp(packageName: String, profileId: Int)
    fun trackLaunchApp(packageName: String)
    fun trackFreezeApp(packageName: String, frozen: Boolean)
    fun trackCreateProfile(profileName: String)
    fun trackDeleteProfile(profileId: Int)
    fun trackShizukuPermission(granted: Boolean)
    fun trackScreenView(screenName: String)
}

class TrackUserEventUseCase @Inject constructor(
    private val tracker: UserEventTracker
) {
    fun cloneApp(packageName: String, profileId: Int, success: Boolean) =
        tracker.trackCloneApp(packageName, profileId, success)

    fun deleteApp(packageName: String, profileId: Int) =
        tracker.trackDeleteApp(packageName, profileId)

    fun launchApp(packageName: String) =
        tracker.trackLaunchApp(packageName)

    fun freezeApp(packageName: String, frozen: Boolean) =
        tracker.trackFreezeApp(packageName, frozen)

    fun createProfile(profileName: String) =
        tracker.trackCreateProfile(profileName)

    fun deleteProfile(profileId: Int) =
        tracker.trackDeleteProfile(profileId)

    fun shizukuPermission(granted: Boolean) =
        tracker.trackShizukuPermission(granted)

    fun screenView(screenName: String) =
        tracker.trackScreenView(screenName)
}
