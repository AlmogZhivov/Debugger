Feature: Stop & Invalid States

  Scenario: four users request to debug, two users stop the program, and two users try invalid commands
    # alex stops on breakpoint, ron stops on sync state
    # avishai stops on breakpoint, tal stops on sync state
    # alex and tal request to stop their session
    # avishai tries to click next sync on breakpoint
    # ron tries to click continue on sync state

    Given user alex has connected with userId 4655ae8e-cdfe-4ce3-ac2b-dc03743a780f
    And user tal has connected with userId d1b10451-bbb0-43e2-b5ad-d10afa73449e
    And user avishai has connected with userId 23c125e3-2ab1-4111-ad4a-0a5de594ae26
    And user ron has connected with userId fddc28aa-e203-40c0-989d-276e0eabf04c

    Given alex has connected to websocket with alex-session
    And ron has connected to websocket with ron-session

    When alex asks to debug with filename testFile1 and toggleMuteBreakpoints false and toggleMuteSyncPoints true and toggleWaitForExternalEvent false and breakpoints 10,21
    Then The debug response should be true with errorCode null and breakpoints 10,21 for user alex
    When ron asks to debug with filename testFile2 and toggleMuteBreakpoints true and toggleMuteSyncPoints false and toggleWaitForExternalEvent false and breakpoints 4,9,11
    Then The debug response should be true with errorCode null and breakpoints 4,9,11 for user ron

    Then wait until user alex has reached status breakpoint
    And wait until user ron has reached status syncstate

    Then alex should get breakpoint notification with BThread {10:bt-world-son}{21:bt-world,bt-world-son}, doubles {10:x=5.0,y=16.7}{21:m=50,n=100,p=150}, strings {10:z=alex} and breakpoint lines 10,21
    And ron should get optional sync state notification wait events [blank]|[blank], blocked events [blank]|[blank], requested events [Thread1-EVENT,Thread2-EVENT]|[Thread1-EVENT,Thread2-EVENT], current event [blank]|[blank], b-threads info list {name:Thread1,requested:[Thread1-EVENT]}{name:Thread2,requested:[Thread2-EVENT]}|{name:Thread2,requested:[Thread2-EVENT]}{name:Thread1,requested:[Thread1-EVENT]}, and events history [blank]|[blank]

    Given avishai has connected to websocket with avishai-session
    And tal has connected to websocket with tal-session

    When avishai asks to debug with filename testFile1 and toggleMuteBreakpoints false and toggleMuteSyncPoints true and toggleWaitForExternalEvent false and breakpoints 10,21
    Then The debug response should be true with errorCode null and breakpoints 10,21 for user avishai
    When tal asks to debug with filename testFile2 and toggleMuteBreakpoints true and toggleMuteSyncPoints false and toggleWaitForExternalEvent false and breakpoints 4,9,11
    Then The debug response should be true with errorCode null and breakpoints 4,9,11 for user tal

    And wait until user avishai has reached status breakpoint
    And wait until user tal has reached status syncstate

    Then avishai should get breakpoint notification with BThread {10:bt-world-son}{21:bt-world,bt-world-son}, doubles {10:x=5.0,y=16.7}{21:m=50,n=100,p=150}, strings {10:z=alex} and breakpoint lines 10,21
    And tal should get optional sync state notification wait events [blank]|[blank], blocked events [blank]|[blank], requested events [Thread1-EVENT,Thread2-EVENT]|[Thread1-EVENT,Thread2-EVENT], current event [blank]|[blank], b-threads info list {name:Thread1,requested:[Thread1-EVENT]}{name:Thread2,requested:[Thread2-EVENT]}|{name:Thread2,requested:[Thread2-EVENT]}{name:Thread1,requested:[Thread1-EVENT]}, and events history [blank]|[blank]

    When alex clicks on stop
    Then The response should be true with errorCode null
    When tal clicks on stop
    Then The response should be true with errorCode null

    When alex clicks on continue
    Then The response should be false with errorCode UNKNOWN_USER
    When tal clicks on next sync
    Then The response should be false with errorCode UNKNOWN_USER

    When avishai clicks on next sync
    Then The response should be false with errorCode NOT_IN_BP_SYNC_STATE
    When avishai clicks on continue
    Then The response should be true with errorCode null
    When ron clicks on continue
    Then The response should be false with errorCode NOT_IN_JS_DEBUG_STATE
    When ron clicks on next sync
    Then The response should be true with errorCode null

    Then wait until user avishai has reached status breakpoint
    And wait until user ron has reached status syncstate

    Then avishai should get breakpoint notification with BThread {10:bt-world-son}{21:bt-world,bt-world-son}, doubles {10:x=5.0,y=16.7}{21:m=50,n=100,p=150}, strings {10:z=alex} and breakpoint lines 10,21
    Then ron should get optional sync state notification wait events [blank]|[blank], blocked events [blank]|[blank], requested events [Thread2-EVENT]|[Thread1-EVENT], current event [blank]|[blank], b-threads info list {name:Thread2,requested:[Thread2-EVENT]}|{name:Thread1,requested:[Thread1-EVENT]}, and events history [Thread1-EVENT]|[Thread2-EVENT]

    When avishai clicks on continue
    Then The response should be true with errorCode null
    When ron clicks on next sync
    Then The response should be true with errorCode null

    Then wait until user avishai has reached status breakpoint
    And wait until user ron has reached status syncstate

    Then avishai should get breakpoint notification with BThread {10:bt-world-son}{21:bt-world,bt-world-son}, doubles {10:x=5.0,y=16.7}{21:m=50,n=100,p=150}, strings {10:z=alex} and breakpoint lines 10,21
    Then ron should get optional sync state notification wait events [blank]|[blank], blocked events [blank]|[blank], requested events [blank]|[blank], current event [blank]|[blank], b-threads info list [blank]|[blank], and events history [Thread1-EVENT,Thread2-EVENT]|[Thread2-EVENT,Thread1-EVENT]

    When avishai toggles mute breakpoints to true
    Then The response should be true with errorCode null
    When ron toggles mute sync states to true
    Then The response should be true with errorCode null

    When avishai clicks on continue
    Then The response should be true with errorCode null
    When ron clicks on next sync
    Then The response should be true with errorCode null

    Then wait until user avishai has reached status superstepdone
    And wait until user ron has reached status superstepdone
    And wait until program of user avishai is over
    And wait until program of user ron is over