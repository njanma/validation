package njanma.validation;

import org.junit.Test;

import static njanma.validation.ValidationTest.Error.CLIENT_ERROR;
import static njanma.validation.ValidationTest.Error.FATAL_ERROR;
import static njanma.validation.ValidationTest.Error.SERVER_ERROR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ValidationTest {

    @Test
    public void shouldPeekAction() {
        //given
        final RemoteServer remoteServer = mock(RemoteServer.class);
        //when
        Validation.of(CLIENT_ERROR, SERVER_ERROR)
                .peek(__ -> remoteServer.sendErrors());
        //then
        verify(remoteServer, times(2)).sendErrors();
    }

    @Test
    public void sequenceOfValidations() {
        //given
        final RemoteServer remoteServer = mock(RemoteServer.class);
        //when
        Validation.sequence(Validation.of(CLIENT_ERROR), Validation.success(), Validation.of(SERVER_ERROR))
                .forEach(__ -> remoteServer.sendErrors());
        //then
        verify(remoteServer, times(2)).sendErrors();
    }

    @Test
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


    @Test
    public void chainOfValidations() {
        //given
        final RemoteServer server = mock(RemoteServer.class);
        //when
        Validation.chain(() -> Validation.of(CLIENT_ERROR), server::extremelyExpensiveValidation)
                .forEach(__ -> server.sendErrors());
        //then
        verify(server, times(1)).sendErrors();
    }

    @Test
    public void combineValidations() {
        //when
        boolean isEq = Validation.success().combine(Validation.failure(FATAL_ERROR))
                .eq(Validation.failure(FATAL_ERROR).combine(Validation.success()));
        //then
        assertTrue(isEq);
    }

    @Test(expected = RuntimeException.class)
    public void throwExceptionOnErrorValidation() {
        Validation.success().combine(Validation.failure(SERVER_ERROR))
                .ifPresentThrow(errs -> new RuntimeException(errs.mkString()));
    }

    @Test
    public void ifPresentAction() {
        //given
        final RemoteServer remoteServer = mock(RemoteServer.class);
        //when
        Validation.of(CLIENT_ERROR, SERVER_ERROR)
                .ifPresent(__ -> remoteServer.sendErrors());
        //then
        verify(remoteServer, times(1)).sendErrors();
    }

    @Test
    public void map() {
        assertEquals(Validation.failure(CLIENT_ERROR).map(Enum::name).get(), CLIENT_ERROR.name());
    }

    @Test
    public void flatMap() {
        boolean isEq = Validation.failure(CLIENT_ERROR)
                .eq(Validation.failure(SERVER_ERROR).flatMap(__ -> Validation.failure(CLIENT_ERROR)));
        assertTrue(isEq);
    }

    enum Error {
        CLIENT_ERROR,
        SERVER_ERROR,
        FATAL_ERROR,
    }

    interface RemoteServer {
        void sendErrors();

        Validation<Error> extremelyLongAction();

        Validation<Error> extremelyExpensiveValidation();
    }
}