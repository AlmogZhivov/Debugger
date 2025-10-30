Feature: External Events

  Scenario: user debugs and adds external events
    Given user alex has connected with userId 4655ae8e-cdfe-4ce3-ac2b-dc03743a780f

    Given alex has connected to websocket with alex-session

    When alex asks to debug with filename testFile4 and toggleMuteBreakpoints true and toggleMuteSyncPoints true and toggleWaitForExternalEvent true and breakpoints [blank]
    Then The debug response should be true with errorCode null and breakpoints [blank] for user alex

    Then wait until user alex has reached status waiting for external event
    And alex should get sync state notification wait events [blank], blocked events [developers], requested events [developers], external events [blank], current event [blank], b-threads info list {name:bt-1,requested:[developers]}{name:bt-2,blocked:[developers]}{name:bt-3}{name:bt-4}{name:bt-5}, and events history [blank]

    When alex adds an external event junk-event-1
    Then The response should be true with errorCode null

    Then wait until user alex has reached status waiting for external event
    And alex should get sync state notification wait events [blank], blocked events [developers], requested events [developers], external events [blank], current event [blank], b-threads info list {name:bt-1,requested:[developers]}{name:bt-2,blocked:[developers]}{name:bt-3}{name:bt-4}{name:bt-5}, and events history [junk-event-1]

    When alex adds an external event from
    Then The response should be true with errorCode null

    Then wait until user alex has reached status waiting for external event
    And alex should get sync state notification wait events [blank], blocked events [developers], requested events [developers], external events [from], current event [blank], b-threads info list {name:bt-1,requested:[developers]}{name:bt-2,blocked:[developers]}{name:bt-3}{name:bt-4}{name:bt-5}, and events history [junk-event-1]

    When alex adds an external event junk-event-2
    Then The response should be true with errorCode null

    Then wait until user alex has reached status waiting for external event
    And alex should get sync state notification wait events [blank], blocked events [developers], requested events [developers], external events [from], current event [blank], b-threads info list {name:bt-1,requested:[developers]}{name:bt-2,blocked:[developers]}{name:bt-3}{name:bt-4}{name:bt-5}, and events history [junk-event-2,junk-event-1]

    When alex adds an external event israel
    Then The response should be true with errorCode null

    Then wait until user alex has reached status waiting for external event
    And alex should get sync state notification wait events [blank], blocked events [developers], requested events [developers], external events [israel,from], current event [blank], b-threads info list {name:bt-1,requested:[developers]}{name:bt-2,blocked:[developers]}{name:bt-3}{name:bt-4}{name:bt-5}, and events history [junk-event-2,junk-event-1]

    When alex adds an external event hello
    Then The response should be true with errorCode null

    Then wait until user alex has reached status waiting for external event
    And alex should get sync state notification wait events [blank], blocked events [developers], requested events [developers], external events [israel,from], current event [blank], b-threads info list {name:bt-1,requested:[developers]}{name:bt-2,blocked:[developers]}{name:bt-3}{name:bt-4}, and events history [hello,junk-event-2,junk-event-1]

    When alex toggles mute sync states to false
    Then The response should be true with errorCode null

    When alex adds an external event world
    Then The response should be true with errorCode null

    Then wait until user alex has reached status syncstate

    When alex toggles wait for external event to false
    Then The response should be true with errorCode null

    And alex should get sync state notification wait events [blank], blocked events [developers], requested events [developers], external events [israel,from], current event [blank], b-threads info list {name:bt-1,requested:[developers]}{name:bt-2,blocked:[developers]}{name:bt-3}, and events history [world,hello,junk-event-2,junk-event-1]

    When alex clicks on next sync
    Then The response should be true with errorCode null

    Then wait until user alex has reached status syncstate
    And alex should get sync state notification wait events [blank], blocked events [developers], requested events [developers], external events [israel], current event [blank], b-threads info list {name:bt-1,requested:[developers]}{name:bt-2,blocked:[developers]}, and events history [from,world,hello,junk-event-2,junk-event-1]

    When alex clicks on next sync
    Then The response should be true with errorCode null

    Then wait until user alex has reached status syncstate
    And alex should get sync state notification wait events [blank], blocked events [blank], requested events [developers], external events [blank], current event [blank], b-threads info list {name:bt-1,requested:[developers]}, and events history [israel,from,world,hello,junk-event-2,junk-event-1]

    When alex clicks on next sync
    Then The response should be true with errorCode null

    Then wait until user alex has reached status syncstate
    And alex should get sync state notification wait events [blank], blocked events [blank], requested events [blank], external events [blank], current event [blank], b-threads info list [blank], and events history [developers,israel,from,world,hello,junk-event-2,junk-event-1]

    When alex clicks on next sync
    Then The response should be true with errorCode null

    Then wait until user alex has reached status superstepdone
    And wait until program of user alex is over