package article.demo.service;

import article.demo.domain.Board;
import article.demo.domain.BoardComment;
import article.demo.domain.Member;
import article.demo.dto.BoardDto;
import article.demo.repository.BoardCommentRepository;
import article.demo.repository.BoardRepository;
import article.demo.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final BoardCommentRepository boardCommentRepository;

    /**
     * 게시글 생성
     */
    @Transactional
    public void saveBoard(BoardDto boardDto, String username) {
        if (username == null || username.isEmpty()) {
            boardDto.boardSetting("익명", 1L);
        } else {
            boardDto.boardSetting(username, 1L);
        }

        Board board = boardDto.toEntity();

        boardRepository.save(board);
    }

    /**
     * 게시글 리스트 조회
     */
    public Board findById(Long id,String username) {
        Board board = boardRepository.findById(id).orElseThrow((() ->
                new IllegalStateException("해당 게시글이 존재하지 않습니다")));
        sessionValidation(username,id);
        return board;
    }

    @Transactional
    public Board updateVisit(Long id) {
        Board board = boardRepository.findById(id).orElseThrow((() ->
                new IllegalStateException("해당 게시글이 존재하지 않습니다")));

        Long countVisit = board.getCountVisit() + 1L;

        BoardDto boardDto = BoardDto.builder()
                .countVisit(countVisit)
                .build();

        board.updateVisit(boardDto.getCountVisit());
        return board;
    }


    public List<Board> findBoardByUsername(String username) {
        List<Board> userBoards = boardRepository.findByCreatedByOrderByIdDesc(username);
        return userBoards;
    }

    public Page<Board> searchBoard(String searchText, String searchType, Pageable pageable) {
        Page<Board> boards;
        if ("title".equals(searchType)) {
            boards = boardRepository.findByTitleContaining(searchText, pageable); // 제목으로 검색
        } else if ("content".equals(searchType)) {
            boards = boardRepository.findByContentContaining(searchText, pageable); // 내용으로 검색
        } else if ("createdBy".equals(searchType)) {
            boards = boardRepository.findByCreatedByContaining(searchText, pageable); // 작성자로 검색
        } else {
            boards = boardRepository.findByTitleContainingOrContentContaining(searchText, searchText, pageable); // 전체 검색
        }
        return boards;
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public void updateBoard(Long id, BoardDto boardDto, String username) {
        sessionValidation(username,id);
        Board board = boardRepository.findById(id).orElseThrow((() ->
                new IllegalStateException("해당 게시글이 존재하지 않습니다")));
        board.updateBoard(boardDto.getTitle(), boardDto.getContent());
        boardRepository.save(board);
    }


    /**
     * 게시글 삭제
     */
    @Transactional
    public void deleteBoard(Long id, String username) {
        sessionValidation(username,id);

        List<BoardComment> comments = boardCommentRepository.findByBoardId(id);
        boardCommentRepository.deleteAll(comments);

        boardRepository.deleteById(id);
    }


    /**
     * 게시판 작성자 검증
     */
    public void sessionValidation(String username,Long id){
        Optional<Member> sessionMemberOptional = memberRepository.findByUsername(username);

        if (sessionMemberOptional.isPresent()) {
            Optional<Board> boardOptional = boardRepository.findById(id);
            if (boardOptional.isPresent()) {
                Board board = boardOptional.get();
                String createdBy = board.getCreatedBy();
                if (createdBy.equals(username) || username.equals("admin")) {
                } else {
                    throw new IllegalStateException("작성자가 아닙니다.");
                }
            } else {
                throw new IllegalArgumentException("해당 게시글이 존재하지 않습니다.");
            }
        } else {
            throw new IllegalStateException("로그인 정보가 없습니다.");
        }
    }
}
