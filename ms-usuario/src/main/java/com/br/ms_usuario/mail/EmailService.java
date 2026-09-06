package com.br.ms_usuario.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarEmailRedefinicaoSenha(String destinatario, String token) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(destinatario);
        mensagem.setSubject("Recuperação de senha - House Task Manager");
        mensagem.setText("""
                Você solicitou a redefinição da sua senha.

                Use o código abaixo para definir uma nova senha (válido por 30 minutos):

                %s

                Se você não solicitou isso, apenas ignore este email.
                """.formatted(token));

        mailSender.send(mensagem);
    }
}
