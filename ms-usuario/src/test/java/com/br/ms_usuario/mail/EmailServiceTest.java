package com.br.ms_usuario.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void enviarEmailRedefinicaoSenha_deveMontarMensagemComOToken() {
        ReflectionTestUtils.setField(emailService, "remetente", "nao-responda@casa.com");

        emailService.enviarEmailRedefinicaoSenha("ana@casa.com", "token-123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage mensagem = captor.getValue();
        assertThat(mensagem.getTo()).containsExactly("ana@casa.com");
        assertThat(mensagem.getFrom()).isEqualTo("nao-responda@casa.com");
        assertThat(mensagem.getText()).contains("token-123");
    }
}
