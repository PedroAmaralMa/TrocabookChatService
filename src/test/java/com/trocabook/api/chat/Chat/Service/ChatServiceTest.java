package com.trocabook.api.chat.Chat.Service;

import com.trocabook.api.chat.Chat.Model.DTO.MensagemDTO;
import com.trocabook.api.chat.Chat.Model.Mensagem;
import com.trocabook.api.chat.Chat.Model.MensagemBackup;
import com.trocabook.api.chat.Chat.Repository.MensagemBackupRepository;
import com.trocabook.api.chat.Chat.Repository.MensagemRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private MensagemRepository mensagemRepository;

    @Mock
    private MensagemBackupRepository mensagemBackupRepository;

    private IChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(mensagemRepository, mensagemBackupRepository);
    }

    @Test
    @DisplayName("CT01 - Quando enviar mensagem com dados válidos, deve persistir no MongoDB e retornar DTO preenchido")
    void quandoEnviarMensagemValida_entaoPersisteComSucesso() {
        // DADO que as informações da mensagem a ser enviada são válidas
        MensagemDTO dtoEntrada = new MensagemDTO();
        dtoEntrada.setUidRemetente("user1");
        dtoEntrada.setUidDestinatario("user2");
        dtoEntrada.setUidAnuncio("anuncio123");
        dtoEntrada.setConteudo("Olá, tenho interesse no livro.");

        ObjectId idGerado = new ObjectId();

        doAnswer(invocation -> {
            Mensagem m = invocation.getArgument(0);
            m.setId(idGerado);
            return m;
        }).when(mensagemRepository).save(any(Mensagem.class));

        // QUANDO o serviço processa o envio da mensagem
        MensagemDTO resultado = chatService.enviarMensagem(dtoEntrada);

        // ENTÃO a mensagem é persistida e os dados do DTO resultante contêm o ID e data de envio
        assertNotNull(resultado);
        assertEquals(idGerado.toHexString(), resultado.getId());
        assertEquals("Olá, tenho interesse no livro.", resultado.getConteudo());
        assertEquals("user1", resultado.getUidRemetente());
        assertEquals("user2", resultado.getUidDestinatario());
        assertNotNull(resultado.getDataEnvio());
        verify(mensagemRepository, times(1)).save(any(Mensagem.class));
    }

    @Test
    @DisplayName("CT02 - Quando buscar mensagem por ID existente, deve retornar os detalhes da mensagem")
    void quandoBuscarPorIdExistente_entaoRetornaMensagemDTO() {
        // DADO que existe uma mensagem cadastrada com o ObjectId correspondente
        String idStr = "64a5f2e1bcf123456789abcd";
        ObjectId objectId = new ObjectId(idStr);

        Mensagem mensagemMock = new Mensagem();
        mensagemMock.setId(objectId);
        mensagemMock.setConteudo("Livro disponível?");
        mensagemMock.setUidRemetente("user1");
        mensagemMock.setUidDestinatario("user2");
        mensagemMock.setUidAnuncio("anuncio1");
        mensagemMock.setDataEnvio(LocalDateTime.now());

        when(mensagemRepository.findById(objectId)).thenReturn(Optional.of(mensagemMock));

        // QUANDO o serviço realiza a busca pelo ID
        MensagemDTO resultado = chatService.buscarPorId(idStr);

        // ENTÃO os dados da mensagem são retornados com sucesso
        assertNotNull(resultado);
        assertEquals(idStr, resultado.getId());
        assertEquals("Livro disponível?", resultado.getConteudo());
        assertEquals("user1", resultado.getUidRemetente());
        verify(mensagemRepository, times(1)).findById(objectId);
    }

    @Test
    @DisplayName("CT03 - Quando buscar mensagem por ID inexistente, deve retornar nulo")
    void quandoBuscarPorIdInexistente_entaoRetornaNulo() {
        // DADO que o ID informado não corresponde a nenhuma mensagem cadastrada
        String idStr = "64a5f2e1bcf123456789abcd";
        ObjectId objectId = new ObjectId(idStr);

        when(mensagemRepository.findById(objectId)).thenReturn(Optional.empty());

        // QUANDO o serviço realiza a busca pelo ID
        MensagemDTO resultado = chatService.buscarPorId(idStr);

        // ENTÃO o resultado retornado é nulo
        assertNull(resultado);
        verify(mensagemRepository, times(1)).findById(objectId);
    }

    @Test
    @DisplayName("CT04 - Quando listar mensagens entre usuários com mensagens existentes, deve retornar lista de DTOs")
    void quandoListarMensagensEntreUsuariosExistentes_entaoRetornaLista() {
        // DADO que existem mensagens trocadas entre os dois usuários para o anúncio
        String uidAnuncio = "anuncio123";
        String remetente = "user1";
        String destinatario = "user2";

        Mensagem mensagemMock = new Mensagem();
        mensagemMock.setId(new ObjectId());
        mensagemMock.setUidRemetente(remetente);
        mensagemMock.setUidDestinatario(destinatario);
        mensagemMock.setUidAnuncio(uidAnuncio);
        mensagemMock.setConteudo("Mensagem de negociação");
        mensagemMock.setDataEnvio(LocalDateTime.now());

        when(mensagemRepository.findMensagensEntreUsuarios(uidAnuncio, remetente, destinatario))
                .thenReturn(List.of(mensagemMock));

        // QUANDO o serviço solicita a listagem entre usuários
        List<MensagemDTO> resultado = chatService.listarMensagensEntreUsuarios(uidAnuncio, remetente, destinatario);

        // ENTÃO a lista é retornada contendo os elementos convertidos para DTO
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Mensagem de negociação", resultado.get(0).getConteudo());
        verify(mensagemRepository, times(1)).findMensagensEntreUsuarios(uidAnuncio, remetente, destinatario);
    }

    @Test
    @DisplayName("CT05 - Quando listar mensagens entre usuários sem histórico, deve retornar lista vazia")
    void quandoListarMensagensEntreUsuariosVazia_entaoRetornaListaVazia() {
        // DADO que não há histórico de mensagens entre os usuários
        String uidAnuncio = "anuncio123";
        String remetente = "user1";
        String destinatario = "user2";

        when(mensagemRepository.findMensagensEntreUsuarios(uidAnuncio, remetente, destinatario))
                .thenReturn(Collections.emptyList());

        // QUANDO o serviço solicita a listagem
        List<MensagemDTO> resultado = chatService.listarMensagensEntreUsuarios(uidAnuncio, remetente, destinatario);

        // ENTÃO uma lista vazia não nula é retornada
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        verify(mensagemRepository, times(1)).findMensagensEntreUsuarios(uidAnuncio, remetente, destinatario);
    }

    @Test
    @DisplayName("CT06 - Quando listar mensagens por usuário remetente, deve retornar a lista de DTOs")
    void quandoListarMensagensPorUsuario_entaoRetornaLista() {
        // DADO que existem mensagens enviadas pelo remetente
        String remetente = "user1";
        Mensagem msg = new Mensagem();
        msg.setId(new ObjectId());
        msg.setUidRemetente(remetente);
        msg.setUidDestinatario("user2");
        msg.setConteudo("Pergunta sobre o livro");
        msg.setDataEnvio(LocalDateTime.now());

        when(mensagemRepository.findByUidRemetente(remetente)).thenReturn(List.of(msg));

        // QUANDO o serviço solicita a listagem por remetente
        List<MensagemDTO> resultado = chatService.listarMensagensPorUsuario(remetente);

        // ENTÃO a lista correspondente é retornada
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Pergunta sobre o livro", resultado.get(0).getConteudo());
        verify(mensagemRepository, times(1)).findByUidRemetente(remetente);
    }

    @Test
    @DisplayName("CT07 - Quando listar mensagens por usuário em ordem decrescente, deve agrupar por conversa cobrindo ambos os lados e deduplicação")
    void quandoListarMensagensPorUsuarioDataEnvioDecrescente_entaoAgrupaConversasEDeduplica() {
        // DADO um conjunto de mensagens onde o usuário é remetente em algumas e destinatário em outras, com conversas duplicadas
        String usuarioLogado = "userPrincipal";

        // Mensagem 1: usuárioLogado é remetente (outro = userA, anúncio = anuncio1)
        Mensagem m1 = new Mensagem();
        m1.setId(new ObjectId());
        m1.setUidRemetente(usuarioLogado);
        m1.setUidDestinatario("userA");
        m1.setUidAnuncio("anuncio1");
        m1.setConteudo("Mais recente com userA");
        m1.setDataEnvio(LocalDateTime.now());

        // Mensagem 2: duplicada na conversa com userA e anuncio1 (deve ser ignorada pelo putIfAbsent)
        Mensagem m2 = new Mensagem();
        m2.setId(new ObjectId());
        m2.setUidRemetente("userA");
        m2.setUidDestinatario(usuarioLogado);
        m2.setUidAnuncio("anuncio1");
        m2.setConteudo("Mensagem antiga com userA");
        m2.setDataEnvio(LocalDateTime.now().minusHours(1));

        // Mensagem 3: usuárioLogado é destinatário (outro = userB, anúncio = anuncio2)
        Mensagem m3 = new Mensagem();
        m3.setId(new ObjectId());
        m3.setUidRemetente("userB");
        m3.setUidDestinatario(usuarioLogado);
        m3.setUidAnuncio("anuncio2");
        m3.setConteudo("Mensagem de userB para userPrincipal");
        m3.setDataEnvio(LocalDateTime.now().minusMinutes(10));

        when(mensagemRepository.findByUidRemetenteOrUidDestinatarioOrderByDataEnvioDesc(usuarioLogado, usuarioLogado))
                .thenReturn(List.of(m1, m2, m3));

        // QUANDO o serviço busca as conversas mais recentes agrupadas
        List<MensagemDTO> resultado = chatService.listarMensagensPorUsuarioDataEnvioDecrescente(usuarioLogado);

        // ENTÃO apenas as 2 conversas distintas são retornadas (deduplicando m2)
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("Mais recente com userA", resultado.get(0).getConteudo());
        assertEquals("Mensagem de userB para userPrincipal", resultado.get(1).getConteudo());
        verify(mensagemRepository, times(1))
                .findByUidRemetenteOrUidDestinatarioOrderByDataEnvioDesc(usuarioLogado, usuarioLogado);
    }

    @Test
    @DisplayName("CT08 - Quando remover mensagem existente, deve salvar backup de EXCLUSAO e deletar mensagem")
    void quandoRemoverMensagemExistente_entaoSalvaBackupEDeleta() {
        // DADO que existe uma mensagem para remoção
        String idStr = "64a5f2e1bcf123456789abcd";
        ObjectId objectId = new ObjectId(idStr);

        Mensagem mensagemMock = new Mensagem();
        mensagemMock.setId(objectId);
        mensagemMock.setConteudo("Mensagem a deletar");

        when(mensagemRepository.findById(objectId)).thenReturn(Optional.of(mensagemMock));

        // QUANDO o serviço remove a mensagem
        boolean resultado = chatService.removerMensagem(idStr);

        // ENTÃO o backup com tipo EXCLUSAO é persistido, a mensagem é deletada e retorna true
        assertTrue(resultado);

        ArgumentCaptor<MensagemBackup> backupCaptor = ArgumentCaptor.forClass(MensagemBackup.class);
        verify(mensagemBackupRepository, times(1)).save(backupCaptor.capture());
        assertEquals(MensagemBackup.TipoBackup.EXCLUSAO, backupCaptor.getValue().getTipoBackup());

        verify(mensagemRepository, times(1)).deleteById(objectId);
    }

    @Test
    @DisplayName("CT09 - Quando tentar remover mensagem inexistente, deve retornar falso e não realizar ações")
    void quandoRemoverMensagemInexistente_entaoRetornaFalso() {
        // DADO que o ID informado não existe no banco
        String idStr = "64a5f2e1bcf123456789abcd";
        ObjectId objectId = new ObjectId(idStr);

        when(mensagemRepository.findById(objectId)).thenReturn(Optional.empty());

        // QUANDO o serviço tenta remover a mensagem
        boolean resultado = chatService.removerMensagem(idStr);

        // ENTÃO nenhuma exclusão ou backup é realizado e retorna falso
        assertFalse(resultado);
        verify(mensagemBackupRepository, never()).save(any());
        verify(mensagemRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("CT10 - Quando alterar mensagem existente, deve salvar backup de ALTERACAO, atualizar e retornar DTO")
    void quandoAlterarMensagemExistente_entaoSalvaBackupEAtualiza() {
        // DADO que existe uma mensagem original a ser alterada
        String idStr = "64a5f2e1bcf123456789abcd";
        ObjectId objectId = new ObjectId(idStr);

        Mensagem mensagemMock = new Mensagem();
        mensagemMock.setId(objectId);
        mensagemMock.setConteudo("Texto anterior");

        when(mensagemRepository.findById(objectId)).thenReturn(Optional.of(mensagemMock));
        when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // QUANDO o serviço altera o conteúdo
        MensagemDTO resultado = chatService.alterarMensagem(idStr, "Texto novo");

        // ENTÃO o backup é do tipo ALTERACAO, o conteúdo é modificado e o DTO retornado reflete a mudança
        assertNotNull(resultado);
        assertEquals("Texto novo", resultado.getConteudo());

        ArgumentCaptor<MensagemBackup> backupCaptor = ArgumentCaptor.forClass(MensagemBackup.class);
        verify(mensagemBackupRepository, times(1)).save(backupCaptor.capture());
        assertEquals(MensagemBackup.TipoBackup.ALTERACAO, backupCaptor.getValue().getTipoBackup());

        verify(mensagemRepository, times(1)).save(any(Mensagem.class));
    }

    @Test
    @DisplayName("CT11 - Quando tentar alterar mensagem inexistente, deve retornar nulo")
    void quandoAlterarMensagemInexistente_entaoRetornaNulo() {
        // DADO que o ID informado não existe para alteração
        String idStr = "64a5f2e1bcf123456789abcd";
        ObjectId objectId = new ObjectId(idStr);

        when(mensagemRepository.findById(objectId)).thenReturn(Optional.empty());

        // QUANDO o serviço tenta alterar a mensagem
        MensagemDTO resultado = chatService.alterarMensagem(idStr, "Novo conteúdo");

        // ENTÃO o resultado é nulo e nenhum salvamento ocorre
        assertNull(resultado);
        verify(mensagemBackupRepository, never()).save(any());
        verify(mensagemRepository, never()).save(any());
    }
}
