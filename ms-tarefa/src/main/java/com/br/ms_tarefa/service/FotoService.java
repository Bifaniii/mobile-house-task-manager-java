package com.br.ms_tarefa.service;

import com.br.ms_tarefa.domain.FotoTarefa;
import com.br.ms_tarefa.domain.Status;
import com.br.ms_tarefa.domain.Tarefa;
import com.br.ms_tarefa.repository.FotoTarefaRepository;
import com.br.ms_tarefa.repository.TarefaRepository;
import com.br.ms_tarefa.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class FotoService {

    static final int TAMANHO_MAXIMO = 2 * 1024 * 1024;

    private final FotoTarefaRepository repository;
    private final TarefaRepository tarefaRepository;
    private final JwtService jwtService;

    public FotoService(FotoTarefaRepository repository, TarefaRepository tarefaRepository, JwtService jwtService) {
        this.repository = repository;
        this.tarefaRepository = tarefaRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public void salvar(UUID tarefaId, FotoTarefa.Tipo tipo, MultipartFile arquivo, String auth) {
        Tarefa tarefa = buscarTarefa(tarefaId, auth);
        if (!tarefa.getUsuarioId().equals(jwtService.autenticado(auth))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só quem faz a tarefa pode enviar a foto");
        }
        if (tarefa.getStatus() == Status.CONCLUIDA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta tarefa já foi concluída");
        }

        byte[] conteudo = lerConteudo(arquivo);

        FotoTarefa foto = repository.findByTarefaIdAndTipo(tarefaId, tipo).orElseGet(FotoTarefa::new);
        foto.setTarefaId(tarefaId);
        foto.setTipo(tipo);
        foto.setConteudo(conteudo);
        foto.setContentType(tipoDaImagem(conteudo));
        repository.save(foto);
    }

    public List<FotoTarefa.Tipo> listar(UUID tarefaId, String auth) {
        buscarTarefa(tarefaId, auth);
        return repository.tiposDaTarefa(tarefaId);
    }

    public FotoTarefa abrir(UUID tarefaId, FotoTarefa.Tipo tipo, String auth) {
        buscarTarefa(tarefaId, auth);
        return repository.findByTarefaIdAndTipo(tarefaId, tipo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto não encontrada"));
    }

    private Tarefa buscarTarefa(UUID tarefaId, String auth) {
        UUID familia = jwtService.familiaDe(auth);
        return tarefaRepository.findById(tarefaId)
                .filter(t -> familia.equals(t.getFamiliaId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada"));
    }

    private byte[] lerConteudo(MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A foto veio vazia");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "A foto precisa ter no máximo 2 MB");
        }
        try {
            byte[] conteudo = arquivo.getBytes();
            tipoDaImagem(conteudo);
            return conteudo;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler a foto");
        }
    }

    /** Confia nos bytes, não no que o cliente declara: o arquivo é devolvido depois a outros aparelhos. */
    static String tipoDaImagem(byte[] b) {
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Envie uma foto em JPEG ou PNG");
    }
}
