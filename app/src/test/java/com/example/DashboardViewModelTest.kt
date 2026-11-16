package com.example

import com.example.model.AdventureDestinations
import com.example.model.CallStatus
import com.example.model.RideMode
import com.example.model.SwitchgearFocus
import com.example.model.TripId
import com.example.model.TurnSignalState
import com.example.viewmodel.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun gearShiftBlockedWhenSideStandDown() {
        val vm = DashboardViewModel(repository = null, audioBridge = null)
        vm.onToggleSideStand()
        assertTrue(vm.uiState.value.sideStandDown)

        vm.onGearUp()
        assertEquals(0, vm.uiState.value.gear)
        assertNotNull(vm.uiState.value.interlockAlert)

        vm.onToggleSideStand()
        vm.onGearUp()
        assertEquals(1, vm.uiState.value.gear)
    }

    @Test
    fun rideModeCyclesAndUpdatesDteAndAbs() {
        val vm = DashboardViewModel(repository = null, audioBridge = null)
        val roadDte = vm.uiState.value.distanceToEmptyKm
        assertEquals(RideMode.ROAD, vm.uiState.value.rideMode)

        vm.onCycleRideMode()
        assertEquals(RideMode.RAIN, vm.uiState.value.rideMode)
        assertTrue(vm.uiState.value.distanceToEmptyKm >= roadDte)

        vm.onCycleRideMode()
        assertEquals(RideMode.OFF_ROAD, vm.uiState.value.rideMode)
        assertEquals("REAR ABS OFF", vm.uiState.value.rideMode.absSummary)
        assertTrue(vm.uiState.value.distanceToEmptyKm < roadDte)
    }

    @Test
    fun hazardToggleAndCallAttendViaHandlebarInputs() {
        val vm = DashboardViewModel(repository = null, audioBridge = null)
        vm.onToggleHazard()
        assertEquals(TurnSignalState.HAZARD, vm.uiState.value.turnSignal)

        vm.onTriggerIncomingCall()
        assertEquals(CallStatus.INCOMING, vm.uiState.value.callState.status)

        vm.onSwitchgearSet()
        assertEquals(CallStatus.ACTIVE, vm.uiState.value.callState.status)

        vm.onSwitchgearBack()
        assertEquals(CallStatus.IDLE, vm.uiState.value.callState.status)
    }

    @Test
    fun tripSwitchingAndResetAndBrightnessAdjustment() {
        val vm = DashboardViewModel(repository = null, audioBridge = null)
        assertEquals(TripId.A, vm.uiState.value.activeTrip)
        vm.toggleActiveTrip()
        assertEquals(TripId.B, vm.uiState.value.activeTrip)

        vm.onResetActiveTrip()
        assertEquals(0.0, vm.uiState.value.tripBKm, 0.001)

        vm.onSelectFocusModule(SwitchgearFocus.BRIGHTNESS)
        vm.onSwitchgearDown()
        assertEquals(0.75f, vm.uiState.value.screenBrightness, 0.01f)
    }

    @Test
    fun bikeButtonsOpenPickerBrowseDestinationsAndStepTurnByTurn() = runTest {
        val vm = DashboardViewModel(repository = null, audioBridge = null)
        assertEquals(SwitchgearFocus.NAVIGATION, vm.uiState.value.focusedModule)

        // Press SET on NAVIGATION opens Destination Picker
        vm.onSwitchgearSet()
        assertTrue(vm.uiState.value.navigationState.showDestinationPicker)
        assertEquals(0, vm.uiState.value.navigationState.highlightedDestinationIndex)

        // Press DOWN to highlight 2nd adventure route
        vm.onSwitchgearDown()
        assertEquals(1, vm.uiState.value.navigationState.highlightedDestinationIndex)

        // Press SET to start turn-by-turn navigation
        vm.onSwitchgearSet()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.navigationState.showDestinationPicker)
        assertTrue(vm.uiState.value.navigationState.isActive)
        assertEquals(AdventureDestinations.presets[1].id, vm.uiState.value.navigationState.destination?.id)
        assertEquals(0, vm.uiState.value.navigationState.currentStepIndex)

        // Press RIGHT to preview next turn step
        vm.onSwitchgearRight()
        assertEquals(1, vm.uiState.value.navigationState.currentStepIndex)

        // Press LEFT to go back to previous turn step
        vm.onSwitchgearLeft()
        assertEquals(0, vm.uiState.value.navigationState.currentStepIndex)

        // Press BACK twice: lock menu, then stop navigation
        vm.onSwitchgearBack()
        assertFalse(vm.uiState.value.isFocusEditing)
        vm.onSwitchgearBack()
        assertFalse(vm.uiState.value.navigationState.isActive)
    }
}
