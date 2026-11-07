package com.br.ms_usuario.service;

import com.br.ms_usuario.domain.Familia;
import com.br.ms_usuario.domain.Parentesco;
import com.br.ms_usuario.domain.dto.FamiliaResponseDTO;
import com.br.ms_usuario.exceptions.RegraDeNegocioException;
import com.br.ms_usuario.repository.FamiliaRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;

@Service
public class FamiliaService {

    // Sem 0/O e 1/I, que se confundem ao ditar o código.
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int TAMANHO_CODIGO = 8;

    private final FamiliaRepository repository;
    private final SecureRandom aleatorio = new SecureRandom();

    public FamiliaService(FamiliaRepository repository) {
        this.repository = repository;
    }

    public Familia criar(String nome) {
        return repository.save(Familia.builder()
                .id(UUID.randomUUID())
                .nome(nome.trim())
                .codigoConvite(novoCodigo())
                .build());
    }

    public void garantirFamiliaLegada() {
        if (!repository.existsById(Familia.ID_LEGADO)) {
            repository.save(Familia.builder()
                    .id(Familia.ID_LEGADO)
                    .nome("Minha família")
                    .codigoConvite(novoCodigo())
                    .build());
        }
    }

    public Familia buscarPorConvite(String codigo) {
        return repository.findByCodigoConvite(codigo.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new RegraDeNegocioException(HttpStatus.BAD_REQUEST,
                        "Código de convite inválido. Peça um novo para quem criou a família."));
    }

    public FamiliaResponseDTO minha(UUID familiaId, Parentesco parentesco) {
        return toDTO(buscar(familiaId), parentesco);
    }

    /** O código antigo deixa de funcionar, para o caso de ter sido compartilhado com quem não devia. */
    @Transactional
    public FamiliaResponseDTO gerarNovoCodigo(UUID familiaId, Parentesco parentesco) {
        Familia familia = buscar(familiaId);
        familia.setCodigoConvite(novoCodigo());
        return toDTO(repository.save(familia), parentesco);
    }

    private Familia buscar(UUID familiaId) {
        return repository.findById(familiaId)
                .orElseThrow(() -> new RegraDeNegocioException(HttpStatus.NOT_FOUND, "Família não encontrada"));
    }

    private FamiliaResponseDTO toDTO(Familia familia, Parentesco parentesco) {
        String codigo = parentesco == Parentesco.FILHO ? null : familia.getCodigoConvite();
        return new FamiliaResponseDTO(familia.getId(), familia.getNome(), codigo);
    }

    private String novoCodigo() {
        String codigo;
        do {
            StringBuilder texto = new StringBuilder(TAMANHO_CODIGO);
            for (int i = 0; i < TAMANHO_CODIGO; i++) {
                texto.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));
            }
            codigo = texto.toString();
        } while (repository.existsByCodigoConvite(codigo));
        return codigo;
    }
}
