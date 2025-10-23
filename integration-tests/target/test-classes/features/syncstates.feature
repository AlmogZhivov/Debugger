Feature: Sync States

  Scenario: four users request to debug with multiple sync states stops
    Given user alex has connected with userId 4655ae8e-cdfe-4ce3-ac2b-dc03743a780f
    And user tal has connected with userId d1b10451-bbb0-43e2-b5ad-d10afa73449e
    And user avishai has connected with userId 23c125e3-2ab1-4111-ad4a-0a5de594ae26
    And user ron has connected with userId fddc28aa-e203-40c0-989d-276e0eabf04c

    Given alex has connected to websocket with alex-session
    And ron has connected to websocket with ron-session

    When alex asks to debug with filename testFile1 and toggleMuteBreakpoints true and toggleMuteSyncPoints false and toggleWaitForExternalEvent false and breakpoints 10,21
    Then The debug response should be true with errorCode null and breakpoints 10,21 for user alex
    When ron asks to debug with filename testFile2 and toggleMuteBreakpoints true and toggleMuteSyncPoints false and toggleWaitForExternalEvent false and breakpoints 4,9,11
    Then The debug response should be true with errorCode null and breakpoints 4,9,11 for user ron

    Then wait until user alex has reached status syncstate
    And wait until user ron has reached status syncstate

    Then alex should get sync state notification wait events [blank], blocked events [blank], requested events son-e, external events [blank], current event [blank], b-threads info list {name:bt-world-son,requested:[son-e]}, and events history [blank]
    And ron should get optional sync state notification wait events [blank]|[blank], blocked events [blank]|[blank], requested events [Thread1-EVENT,Thread2-EVENT]|[Thread1-EVENT,Thread2-EVENT], current event [blank]|[blank], b-threads info list {name:Thread1,requested:[Thread1-EVENT]}{name:Thread2,requested:[Thread2-EVENT]}|{name:Thread2,requested:[Thread2-EVENT]}{name:Thread1,requested:[Thread1-EVENT]}, and events history [blank]|[blank]

    Given avishai has connected to websocket with avishai-session
    And tal has connected to websocket with tal-session

    When avishai asks to debug with filename testFile1 and toggleMuteBreakpoints true and toggleMuteSyncPoints false and toggleWaitForExternalEvent false and breakpoints 10,21
    Then The debug response should be true with errorCode null and breakpoints 10,21 for user avishai
    When tal asks to debug with filename testFile2 and toggleMuteBreakpoints true and toggleMuteSyncPoints false and toggleWaitForExternalEvent false and breakpoints 4,9,11
    Then The debug response should be true with errorCode null and breakpoints 4,9,11 for user tal

    And wait until user avishai has reached status syncstate
    And wait until user tal has reached status syncstate

    Then avishai should get sync state notification wait events [blank], blocked events [blank], requested events son-e, external events [blank], current event [blank], b-threads info list {name:bt-world-son,requested:[son-e]}, and events history [blank]
    And tal should get optional sync state notification wait events [blank]|[blank], blocked events [blank]|[blank], requested events [Thread1-EVENT,Thread2-EVENT]|[Thread1-EVENT,Thread2-EVENT], current event [blank]|[blank], b-threads info list {name:Thread1,requested:[Thread1-EVENT]}{name:Thread2,requested:[Thread2-EVENT]}|{name:Thread2,requested:[Thread2-EVENT]}{name:Thread1,requested:[Thread1-EVENT]}, and events history [blank]|[blank]

    When alex toggles mute sync states to true
    Then The response should be true with errorCode null
    When tal toggles mute sync states to true
    Then The response should be true with errorCode null

    When alex clicks on next sync
    Then The response should be true with errorCode null
    When tal clicks on next sync
    Then The response should be true with errorCode null
    When avishai clicks on next sync
    Then The response should be true with errorCode null
    When ron clicks on next sync
    Then The response should be true with errorCode null

    Then wait until user alex has reached status superstepdone
    And wait until user tal has reached status superstepdone
    And wait until program of user alex is over
    And wait until program of user tal is over
    And wait until user avishai has reached status syncstate
    And wait until user ron has reached status syncstate

    Then avishai should get sync state notification wait events [blank], blocked events [blank], requested events world12121, external events [blank], current event [blank], b-threads info list {name:bt-world-son,requested:[world12121]}, and events history [son-e]
    Then ron should get optional sync state notification wait events [blank]|[blank], blocked events [blank]|[blank], requested events [Thread2-EVENT]|[Thread1-EVENT], current event [blank]|[blank], b-threads info list {name:Thread2,requested:[Thread2-EVENT]}|{name:Thread1,requested:[Thread1-EVENT]}, and events history [Thread1-EVENT]|[Thread2-EVENT]

    When avishai clicks on next sync
    Then The response should be true with errorCode null
    When ron clicks on next sync
    Then The response should be true with errorCode null

    Then wait until user avishai has reached status syncstate
    And wait until user ron has reached status syncstate

    Then avishai should get sync state notification wait events [blank], blocked events [blank], requested events [blank], external events [blank], current event [blank], b-threads info list {}, and events history [world12121,son-e]
    Then ron should get optional sync state notification wait events [blank]|[blank], blocked events [blank]|[blank], requested events [blank]|[blank], current event [blank]|[blank], b-threads info list [blank]|[blank], and events history [Thread1-EVENT,Thread2-EVENT]|[Thread2-EVENT,Thread1-EVENT]

    When avishai toggles mute sync states to true
    Then The response should be true with errorCode null
    When ron toggles mute sync states to true
    Then The response should be true with errorCode null

    When avishai clicks on next sync
    Then The response should be true with errorCode null
    When ron clicks on next sync
    Then The response should be true with errorCode null

    Then wait until user avishai has reached status superstepdone
    And wait until user ron has reached status superstepdone
    And wait until program of user avishai is over
    And wait until program of user ron is over

  Scenario: two users debug and set sync snapshots
    Given user alex has connected with userId 4655ae8e-cdfe-4ce3-ac2b-dc03743a780f
    And user tal has connected with userId d1b10451-bbb0-43e2-b5ad-d10afa73449e

    Given alex has connected to websocket with alex-session

    When alex asks to debug with filename testFile1 and toggleMuteBreakpoints true and toggleMuteSyncPoints false and toggleWaitForExternalEvent false and breakpoints 10,21
    Then The debug response should be true with errorCode null and breakpoints 10,21 for user alex

    Then wait until user alex has reached status syncstate
    Then alex should get sync state notification wait events [blank], blocked events [blank], requested events son-e, external events [blank], current event [blank], b-threads info list {name:bt-world-son,requested:[son-e]}, and events history [blank]

    Given tal has connected to websocket with avishai-session

    When tal asks to debug with filename testFile2 and toggleMuteBreakpoints true and toggleMuteSyncPoints false and toggleWaitForExternalEvent false and breakpoints 4,9,11
    Then The debug response should be true with errorCode null and breakpoints 4,9,11 for user tal
    And wait until user tal has reached status syncstate
    And tal should get optional sync state notification wait events [blank]|[blank], blocked events [blank]|[blank], requested events [Thread1-EVENT,Thread2-EVENT]|[Thread1-EVENT,Thread2-EVENT], current event [blank]|[blank], b-threads info list {name:Thread1,requested:[Thread1-EVENT]}{name:Thread2,requested:[Thread2-EVENT]}|{name:Thread2,requested:[Thread2-EVENT]}{name:Thread1,requested:[Thread1-EVENT]}, and events history [blank]|[blank]

    When alex clicks on next sync
    Then The response should be true with errorCode null
    When tal clicks on next sync
    Then The response should be true with errorCode null

    Then wait until user alex has reached status syncstate
    And alex should get sync state notification wait events [blank], blocked events [blank], requested events world12121, external events [blank], current event [blank], b-threads info list {name:bt-world-son,requested:[world12121]}, and events history [son-e]

    Then wait until user tal has reached status syncstate
    And tal should get optional sync state notification wait events [blank]|[blank], blocked events [blank]|[blank], requested events [Thread2-EVENT]|[Thread1-EVENT], current event [blank]|[blank], b-threads info list {name:Thread2,requested:[Thread2-EVENT]}|{name:Thread1,requested:[Thread1-EVENT]}, and events history [Thread1-EVENT]|[Thread2-EVENT]

    When alex clicks on next sync
    Then The response should be true with errorCode null
    When tal clicks on next sync
    Then The response should be true with errorCode null

    Then wait until user alex has reached status syncstate
     And alex should get sync state notification wait events [blank], blocked events [blank], requested events [blank], external events [blank], current event [blank], b-threads info list {}, and events history [world12121,son-e]

    And wait until user tal has reached status syncstate
    Then tal should get optional sync state notification wait events [blank]|[blank], blocked events [blank]|[blank], requested events [blank]|[blank], current event [blank]|[blank], b-threads info list [blank]|[blank], and events history [Thread1-EVENT,Thread2-EVENT]|[Thread2-EVENT,Thread1-EVENT]

    When alex sets sync snapshot, to the time event son-e was chosen
    Then The response should be true with errorCode null
    When tal sets sync snapshot, before the first event selection
    Then The response should be true with errorCode null

    When alex clicks on next sync
    Then The response should be true with errorCode null
    When tal clicks on next sync
    Then The response should be true with errorCode null

    Then wait until user alex has reached status syncstate
    Then alex should get sync state notification wait events [blank], blocked events [blank], requested events world12121, external events [blank], current event [blank], b-threads info list {name:bt-world-son,requested:[world12121]}, and events history [son-e]
    And wait until user tal has reached status syncstate
    And tal should get optional sync state notification wait events [blank]|[blank], blocked events [blank]|[blank], requested events [Thread2-EVENT]|[Thread1-EVENT], current event [blank]|[blank], b-threads info list {name:Thread2,requested:[Thread2-EVENT]}|{name:Thread1,requested:[Thread1-EVENT]}, and events history [Thread1-EVENT]|[Thread2-EVENT]

    When alex clicks on next sync
    Then The response should be true with errorCode null
    When tal clicks on next sync
    Then The response should be true with errorCode null

    Then wait until user alex has reached status syncstate
    And alex should get sync state notification wait events [blank], blocked events [blank], requested events [blank], external events [blank], current event [blank], b-threads info list {}, and events history [world12121,son-e]
    Then wait until user tal has reached status syncstate
    Then tal should get optional sync state notification wait events [blank]|[blank], blocked events [blank]|[blank], requested events [blank]|[blank], current event [blank]|[blank], b-threads info list [blank]|[blank], and events history [Thread1-EVENT,Thread2-EVENT]|[Thread2-EVENT,Thread1-EVENT]

    When alex clicks on next sync
    Then The response should be true with errorCode null

    Then wait until user alex has reached status superstepdone
    And wait until program of user alex is over
    When tal clicks on next sync
    Then The response should be true with errorCode null

    Then wait until user tal has reached status superstepdone
    And wait until program of user tal is over
