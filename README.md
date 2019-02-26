### Examples of usage:

```
    public void shouldPeekAction() {
        //given
        final RemoteServer remoteServer = mock(RemoteServer.class);
        //when
        Validation.of(CLIENT_ERROR, SERVER_ERROR)
                .peek(__ -> remoteServer.sendErrors());
        //then
        verify(remoteServer, times(2)).sendErrors();
    }

    public void sequenceOfValidations() {
        //given
        final RemoteServer remoteServer = mock(RemoteServer.class);
        //when
        Validation.sequence(Validation.of(CLIENT_ERROR), Validation.success(), Validation.of(SERVER_ERROR))
                .forEach(__ -> remoteServer.sendErrors());
        //then
        verify(remoteServer, times(2)).sendErrors();
    }

    public void parSequenceOfValidations() {
        //given
        final RemoteServer server = mock(RemoteServer.class);
        when(server.extremelyLongAction()).thenReturn(Validation.failure(SERVER_ERROR));
        //when
        Validation.parSequence(() -> Validation.of(CLIENT_ERROR), server::extremelyLongAction)
                .forEach(__ -> server.sendErrors());
        //then
        verify(server, times(2)).sendErrors();
    }


    public void chainOfValidations() {
        //given
        final RemoteServer server = mock(RemoteServer.class);
        //when
        Validation.chain(() -> Validation.of(CLIENT_ERROR), server::extremelyExpensiveValidation)
                .forEach(__ -> server.sendErrors());
        //then
        verify(server, times(1)).sendErrors();
    }

    public void combineValidations() {
        //when
        boolean combineRes = Validation.success().combine(Validation.failure(FATAL_ERROR))
                .eq(Validation.failure(FATAL_ERROR).combine(Validation.success()));
        //then
        assertTrue(combineRes);
    }
```
