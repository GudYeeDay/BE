package org.example.gudyeeday.domain.notification.push;

import org.example.gudyeeday.domain.notification.repository.DeviceTokenRepository;
import org.example.gudyeeday.domain.notification.service.DeviceTokenService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;


class PushServiceTest {

    private final DeviceTokenRepository deviceTokenRepository = mock(DeviceTokenRepository.class);
    private final DeviceTokenService deviceTokenService = mock(DeviceTokenService.class);
    private final PushSender pushSender = mock(PushSender.class);

    private final PushService pushService = new PushService(deviceTokenRepository, deviceTokenService, pushSender);

    private static final Map<String, String> DATA = Map.of("type", "MISSION_REMINDER");

    @Test
    void 사용자의_모든_기기로_같은_문구를_보낸다() {
        when(deviceTokenRepository.findPushTargetsByUserIdIn(List.of(1L, 2L))).thenReturn(List.of(
                new PushTarget(1L, "phone-1"),
                new PushTarget(1L, "tablet-1"),
                new PushTarget(2L, "phone-2")
        ));
        when(pushSender.send(anyList())).thenReturn(new PushSendResult(3, 0, List.of()));

        pushService.send(List.of(
                new PushRequest(1L, "제목1", "내용1", DATA),
                new PushRequest(2L, "제목2", "내용2", DATA)
        ));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PushMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(pushSender).send(captor.capture());
        assertThat(captor.getValue())
                .extracting(PushMessage::token, PushMessage::title)
                .containsExactlyInAnyOrder(
                        tuple("phone-1", "제목1"),
                        tuple("tablet-1", "제목1"),
                        tuple("phone-2", "제목2")
                );
        verify(deviceTokenService, never()).deleteTokens(anyCollection());
    }

    @Test
    void 등록된_기기가_없으면_보내지_않는다() {
        when(deviceTokenRepository.findPushTargetsByUserIdIn(any())).thenReturn(List.of());

        pushService.send(List.of(new PushRequest(1L, "제목", "내용", DATA)));

        verifyNoInteractions(pushSender);
    }

    @Test
    void 만료된_토큰은_삭제한다() {
        when(deviceTokenRepository.findPushTargetsByUserIdIn(any())).thenReturn(List.of(
                new PushTarget(1L, "alive"),
                new PushTarget(1L, "dead")
        ));
        when(pushSender.send(anyList())).thenReturn(new PushSendResult(1, 1, List.of("dead")));

        pushService.send(List.of(new PushRequest(1L, "제목", "내용", DATA)));

        verify(deviceTokenService).deleteTokens(List.of("dead"));
    }

    @Test
    void 기기가_500대를_넘으면_나눠서_보낸다() {
        List<PushTarget> targets = IntStream.range(0, 501)
                .mapToObj(i -> new PushTarget(1L, "token-" + i))
                .toList();
        when(deviceTokenRepository.findPushTargetsByUserIdIn(any())).thenReturn(targets);
        when(pushSender.send(anyList())).thenReturn(new PushSendResult(0, 0, List.of()));

        pushService.send(List.of(new PushRequest(1L, "제목", "내용", DATA)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PushMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(pushSender, times(2)).send(captor.capture());
        assertThat(captor.getAllValues()).extracting(List::size).containsExactly(500, 1);
    }

    @Test
    void 발송_중_예외가_나도_밖으로_던지지_않는다() {
        when(deviceTokenRepository.findPushTargetsByUserIdIn(any())).thenReturn(List.of(new PushTarget(1L, "token")));
        when(pushSender.send(anyList())).thenThrow(new IllegalStateException("FCM 장애"));

        assertThatCode(() -> pushService.send(List.of(new PushRequest(1L, "제목", "내용", DATA))))
                .doesNotThrowAnyException();
    }
}
