package br.com.template.core.modules.user.grpc;

import br.com.template.core.config.security.jwt.services.JwtService;
import br.com.template.core.proto.user.Empty;
import br.com.template.core.proto.user.MeResponse;
import br.com.template.core.proto.user.MeServiceGrpc;
import br.com.template.core.proto.user.MeServiceGrpc.MeServiceBlockingStub;
import br.com.template.core.modules.user.components.UserComponent;
import br.com.template.core.modules.user.models.Role;
import br.com.template.core.modules.user.models.User;
import br.com.template.core.modules.user.repositories.UserRepository;
import br.com.template.core.support.CoreTestServer;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.client.GrpcChannelFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fluxo ponta a ponta da porta gRPC em processo: token de verdade, Flyway + H2 de verdade e a
 * mesma política de token da porta HTTP.
 */
@SpringBootTest
@AutoConfigureTestGrpcTransport
class MeGrpcServiceTest extends CoreTestServer {

    private static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Autowired
    private GrpcChannelFactory grpcChannelFactory;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserComponent userComponent;
    @Autowired
    private JwtService jwtService;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .fullName("Maria da Silva")
                .email("maria" + System.nanoTime() + "@example.com")
                .password("hash")
                .principalRole(Role.ROLE_USER)
                .roles(List.of(Role.ROLE_USER))
                .active(true)
                .emailVerifiedAt(java.time.OffsetDateTime.now())
                .build());
    }

    private MeServiceBlockingStub stub() {
        return MeServiceGrpc.newBlockingStub(grpcChannelFactory.createChannel("local"));
    }

    private MeServiceBlockingStub stubWithToken(String token) {
        Metadata headers = new Metadata();
        headers.put(AUTHORIZATION, "Bearer " + token);

        return stub().withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
    }

    private Status.Code codeOf(Runnable call) {
        try {
            call.run();
            return Status.Code.OK;
        } catch (StatusRuntimeException exception) {
            return exception.getStatus().getCode();
        }
    }

    @Test
    @DisplayName("should return the profile of the caller identified by the bearer token")
    void shouldReturnProfile_whenTokenValid() {
        MeResponse me = stubWithToken(jwtService.generateAccessToken(user)).getMe(Empty.getDefaultInstance());

        assertThat(me.getId()).isEqualTo(user.getId());
        assertThat(me.getEmail()).isEqualTo(user.getEmail());
        assertThat(me.getPrincipalRole()).isEqualTo("ROLE_USER");
        assertThat(me.getPhone()).isEmpty();
    }

    @Test
    @DisplayName("should refuse the call without a token")
    void shouldRefuse_whenNoToken() {
        assertThat(codeOf(() -> stub().getMe(Empty.getDefaultInstance()))).isEqualTo(Status.Code.UNAUTHENTICATED);
    }

    @Test
    @DisplayName("should refuse a garbage token")
    void shouldRefuse_whenTokenInvalid() {
        assertThatThrownBy(() -> stubWithToken("nao-e-um-jwt").getMe(Empty.getDefaultInstance()))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        exception -> assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED));
    }

    @Test
    @DisplayName("should refuse a token from a revoked session, exactly like the http port")
    void shouldRefuse_whenSessionRevoked() {
        String token = jwtService.generateAccessToken(user);

        userComponent.revokeTokens(userRepository.findById(user.getId()).orElseThrow());

        assertThat(codeOf(() -> stubWithToken(token).getMe(Empty.getDefaultInstance())))
                .isEqualTo(Status.Code.UNAUTHENTICATED);
    }
}
