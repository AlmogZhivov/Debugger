Feature: Breakpoints
  Scenario Outline: multiple valid breakpoints
    Given user <username> has connected with userId <userId>
    And <username> has connected to websocket with <sessionId>
    When <username> asks to debug with filename <filename> and toggleMuteBreakpoints <toggleMuteBreakpoints> and toggleMuteSyncPoints <toggleMuteSyncPoints> and toggleWaitForExternalEvent <toggleWaitForExternalEvent> and breakpoints <breakpoints>
    Then The debug response should be true with errorCode null and breakpoints <breakpoints> for user <username>
    And wait until user <username> has reached status breakpoint
    And <username> should get breakpoint notification with BThread <bThreadNameByBreakpoint>, doubles <doubleVariables>, strings <stringVariables> and breakpoint lines <breakpoints>
    When <username> clicks on continue
    Then The response should be true with errorCode null
    And wait until user <username> has reached status breakpoint
    And <username> should get breakpoint notification with BThread <bThreadNameByBreakpoint>, doubles <doubleVariables>, strings <stringVariables> and breakpoint lines <breakpoints>
    When <username> clicks on continue
    Then The response should be true with errorCode null
    And wait until user <username> has reached status breakpoint
    And <username> should get breakpoint notification with BThread <bThreadNameByBreakpoint>, doubles <doubleVariables>, strings <stringVariables> and breakpoint lines <breakpoints>
    And verify all breakpoints of user <username> were reached

    Examples:
      | username | sessionId  | userId                               | filename  | toggleMuteBreakpoints | toggleMuteSyncPoints | bThreadNameByBreakpoint                     | toggleWaitForExternalEvent | breakpoints | stringVariables | doubleVariables                        |
      | alex     | sessionId1 | 4655ae8e-cdfe-4ce3-ac2b-dc03743a780f | testFile1 | false                 | true                 | {10:bt-world-son}{21:bt-world,bt-world-son} | false                      | 10,21       | {10:z=alex}     | {10:x=5.0,y=16.7}{21:m=50,n=100,p=150} |
      | tal      | sessionId2 | d1b10451-bbb0-43e2-b5ad-d10afa73449e | testFile2 | false                 | true                 | {4:Thread1}{9:Thread2}{11:Thread2}          | false                      | 4,9,11      | {11:var2=alex}  | {4:t=6.0}{9:varT2B=1}{11:varT2B=1}     |


  Scenario Outline: toggle mute breakpoints after first breakpoint
    Given user <username> has connected with userId <userId>
    And <username> has connected to websocket with <sessionId>
    When <username> asks to debug with filename <filename> and toggleMuteBreakpoints <toggleMuteBreakpoints> and toggleMuteSyncPoints <toggleMuteSyncPoints> and toggleWaitForExternalEvent <toggleWaitForExternalEvent> and breakpoints <breakpoints>
    Then The debug response should be true with errorCode null and breakpoints <breakpoints> for user <username>
    And wait until user <username> has reached status breakpoint
    And <username> should get breakpoint notification with BThread <bThreadNameByBreakpoint>, doubles <doubleVariables>, strings <stringVariables> and breakpoint lines <breakpoints>
    When <username> toggles mute breakpoints to true
    Then The response should be true with errorCode null
    When <username> clicks on continue
    Then The response should be true with errorCode null
    And wait until program of user <username> is over
    And verify user <username> has reached only 1 breakpoints

    Examples:
      | username | sessionId  | userId                               | filename  | toggleMuteBreakpoints | toggleMuteSyncPoints | bThreadNameByBreakpoint                     | toggleWaitForExternalEvent | breakpoints | stringVariables | doubleVariables                                                           |
      | alex     | sessionId1 | 4655ae8e-cdfe-4ce3-ac2b-dc03743a780f | testFile1 | false                 | true                 | {10:bt-world-son}{21:bt-world,bt-world-son} | flase                      | 10,21       | {10:z=alex}     | {10:x=5.0,y=16.7}{21:m=50,n=100,p=150} |
      | tal      | sessionId2 | d1b10451-bbb0-43e2-b5ad-d10afa73449e | testFile2 | false                 | true                 | {4:Thread1}{9:Thread2}{11:Thread2}          | false                      | 4,9,11      | {11:var2=alex}  | {4:t=6.0}{9:varT2B=1}{11:varT2B=1}     |


  Scenario: four users request to debug with multiple breakpoints
    Given user alex has connected with userId 4655ae8e-cdfe-4ce3-ac2b-dc03743a780f
    And user tal has connected with userId d1b10451-bbb0-43e2-b5ad-d10afa73449e
    And user avishai has connected with userId 23c125e3-2ab1-4111-ad4a-0a5de594ae26
    And user ron has connected with userId fddc28aa-e203-40c0-989d-276e0eabf04c

    Given alex has connected to websocket with alex-session
    And ron has connected to websocket with ron-session

    When alex asks to debug with filename testFile1 and toggleMuteBreakpoints false and toggleMuteSyncPoints true and toggleWaitForExternalEvent false and breakpoints 10,21,30,50,-1,16
    Then The debug response should be true with errorCode null and breakpoints 10,21 for user alex
    When ron asks to debug with filename testFile2 and toggleMuteBreakpoints false and toggleMuteSyncPoints true and toggleWaitForExternalEvent false and breakpoints 4,9,11,30,50,-1,6
    Then The debug response should be true with errorCode null and breakpoints 4,9,11 for user ron

    Then wait until user alex has reached status breakpoint
    And wait until user ron has reached status breakpoint

    Then alex should get breakpoint notification with BThread {10:bt-world-son}{21:bt-world,bt-world-son}, doubles {10:x=5.0,y=16.7}{21:m=50,n=100,p=150}, strings {10:z=alex} and breakpoint lines 10,21
    And ron should get breakpoint notification with BThread {4:Thread1}{9:Thread2}{11:Thread2}, doubles {4:t=6.0}{9:varT2B=1}{11:varT2B=1}, strings {11:var2=alex} and breakpoint lines 4,9,11

    Given avishai has connected to websocket with avishai-session
    And tal has connected to websocket with tal-session

    When avishai asks to debug with filename testFile1 and toggleMuteBreakpoints false and toggleMuteSyncPoints true and toggleWaitForExternalEvent false and breakpoints 10,21,30,50,-1,16
    Then The debug response should be true with errorCode null and breakpoints 10,21 for user avishai
    When tal asks to debug with filename testFile2 and toggleMuteBreakpoints false and toggleMuteSyncPoints true and toggleWaitForExternalEvent false and breakpoints 4,9,11,30,50,-1,6
    Then The debug response should be true with errorCode null and breakpoints 4,9,11 for user tal

    And wait until user avishai has reached status breakpoint
    And wait until user tal has reached status breakpoint

    Then avishai should get breakpoint notification with BThread {10:bt-world-son}{21:bt-world,bt-world-son}, doubles {10:x=5.0,y=16.7}{21:m=50,n=100,p=150}, strings {10:z=alex} and breakpoint lines 10,21
    And tal should get breakpoint notification with BThread {4:Thread1}{9:Thread2}{11:Thread2}, doubles {4:t=6.0}{9:varT2B=1}{11:varT2B=1}, strings {11:var2=alex} and breakpoint lines 4,9,11

    When alex toggles mute breakpoints to true
    Then The response should be true with errorCode null
    When tal toggles mute breakpoints to true
    Then The response should be true with errorCode null

    When alex clicks on continue
    Then The response should be true with errorCode null
    When tal clicks on continue
    Then The response should be true with errorCode null
    When avishai clicks on continue
    Then The response should be true with errorCode null
    When ron clicks on continue
    Then The response should be true with errorCode null

    Then wait until program of user alex is over
    And verify user alex has reached only 1 breakpoints
    And wait until program of user tal is over
    And verify user tal has reached only 1 breakpoints

    And wait until user avishai has reached status breakpoint
    And wait until user ron has reached status breakpoint

    Then avishai should get breakpoint notification with BThread {10:bt-world-son}{21:bt-world,bt-world-son}, doubles {10:x=5.0,y=16.7}{21:m=50,n=100,p=150}, strings {10:z=alex} and breakpoint lines 10,21
    And ron should get breakpoint notification with BThread {4:Thread1}{9:Thread2}{11:Thread2}, doubles {4:t=6.0}{9:varT2B=1}{11:varT2B=1}, strings {11:var2=alex} and breakpoint lines 4,9,11

    When avishai clicks on continue
    Then The response should be true with errorCode null
    When ron clicks on continue
    Then The response should be true with errorCode null

    Then wait until user avishai has reached status breakpoint
    And wait until user ron has reached status breakpoint

    Then avishai should get breakpoint notification with BThread {10:bt-world-son}{21:bt-world,bt-world-son}, doubles {10:x=5.0,y=16.7}{21:m=50,n=100,p=150}, strings {10:z=alex} and breakpoint lines 10,21
    And verify all breakpoints of user avishai were reached

    Then ron should get breakpoint notification with BThread {4:Thread1}{9:Thread2}{11:Thread2}, doubles {4:t=6.0}{9:varT2B=1}{11:varT2B=1}, strings {11:var2=alex} and breakpoint lines 4,9,11
    And verify all breakpoints of user ron were reached