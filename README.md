### Examples of usage

- peek on each:
```
        Validation.of(CLIENT_ERROR, SERVER_ERROR)
                .peek(__ -> remoteServer.sendErrors());
       
        verify(remoteServer, times(2)).sendErrors();
```
- validation sequence:
```
        Validation.sequence(Validation.of(CLIENT_ERROR), Validation.success(), Validation.of(SERVER_ERROR))
                .forEach(__ -> remoteServer.sendErrors());
        
        verify(remoteServer, times(2)).sendErrors();
```
- validation parallel sequence:
```
        Validation.parSequence(() -> Validation.of(CLIENT_ERROR), server::extremelyLongAction)
                .forEach(__ -> server.sendErrors());
        
        verify(server, times(2)).sendErrors();
```
- validation chain:
```
        Validation.chain(() -> Validation.of(CLIENT_ERROR), server::extremelyExpensiveValidation)
                .forEach(__ -> server.sendErrors());
        
        verify(server, times(1)).sendErrors();
```
- map:
```
        assertEquals(Validation.failure(CLIENT_ERROR).map(Enum::name).get(), CLIENT_ERROR.name());
```
- flatMap:
```
        boolean isEq = Validation.failure(CLIENT_ERROR)
                .eq(Validation.failure(SERVER_ERROR).flatMap(__ -> Validation.failure(CLIENT_ERROR)));
        assertTrue(isEq);
```
- combine validations:
```
        boolean combineRes = Validation.success().combine(Validation.failure(FATAL_ERROR))
                .eq(Validation.failure(FATAL_ERROR).combine(Validation.success()));
        
        assertTrue(combineRes);
```
- if present an action:
```
        Validation.of(CLIENT_ERROR, SERVER_ERROR)
                .ifPresent(__ -> remoteServer.sendErrors());
        //then
        verify(remoteServer, times(1)).sendErrors();
```
- throw an exception on error validation:
```
        Validation.success().combine(Validation.failure(SERVER_ERROR))
                .ifPresentThrow(errs -> new RuntimeException(errs.mkString()));
```
