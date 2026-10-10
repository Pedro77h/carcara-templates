package br.com.template.core.modules.user.grpc;

import br.com.template.core.core.components.CurrentUserComponent;
import br.com.template.core.proto.user.Empty;
import br.com.template.core.proto.user.MeResponse;
import br.com.template.core.proto.user.MeServiceGrpc.MeServiceImplBase;
import br.com.template.core.modules.user.components.AccountComponent;
import br.com.template.core.modules.user.dtos.MeDto;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import java.util.Objects;

/**
 * Exemplo de porta gRPC: o mesmo papel de um controller REST — fala só com components.
 * A autorização está em {@code GrpcSecurityConfig}.
 */
@GrpcService
public class MeGrpcService extends MeServiceImplBase {

    private final CurrentUserComponent currentUserComponent;
    private final AccountComponent accountComponent;

    public MeGrpcService(CurrentUserComponent currentUserComponent, AccountComponent accountComponent) {
        this.currentUserComponent = currentUserComponent;
        this.accountComponent = accountComponent;
    }

    @Override
    public void getMe(Empty request, StreamObserver<MeResponse> responseObserver) {
        MeDto me = accountComponent.findProfile(currentUserComponent.getCurrentUserId());

        responseObserver.onNext(MeResponse.newBuilder()
                .setId(me.getId())
                .setFullName(me.getFullName())
                .setEmail(me.getEmail())
                .setPhone(Objects.toString(me.getPhone(), ""))
                .setPrincipalRole(me.getPrincipalRole().name())
                .setEmailVerified(me.isEmailVerified())
                .build());
        responseObserver.onCompleted();
    }
}
