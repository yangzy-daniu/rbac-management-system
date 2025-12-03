package com.example.demo.service;

import com.example.demo.dto.TodoCreateDTO;
import com.example.demo.dto.TodoDTO;
import com.example.demo.entity.Todo;
import com.example.demo.entity.User;
import com.example.demo.repository.TodoRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    // 获取用户待办事项列表
    public List<TodoDTO> getUserTodos(Long userId, Boolean completed, Integer limit) {
        List<Todo> todos;
        if (limit != null && limit > 0) {
            Pageable pageable = PageRequest.of(0, limit);
            Page<Todo> todoPage = todoRepository.findByUserIdAndCompleted(userId, completed, pageable);
            todos = todoPage.getContent();
        } else {
            todos = todoRepository.findByUserIdAndCompletedOrderByDueTimeAsc(userId, completed);
        }

        return todos.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // 获取今日待办
    public List<TodoDTO> getTodayTodos(Long userId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<Todo> todos = todoRepository.findByUserIdAndDueTimeBetweenAndCompleted(
                userId, startOfDay, endOfDay, false);

        return todos.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // 获取高优先级待办
    public List<TodoDTO> getHighPriorityTodos(Long userId) {
        List<Todo> todos = todoRepository.findByUserIdAndPriorityAndCompleted(
                userId, "HIGH", false);

        return todos.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // 创建待办事项
    public TodoDTO createTodo(Long userId, TodoCreateDTO createDTO) {
        Todo todo = new Todo();
        todo.setTitle(createDTO.getTitle());
        todo.setDescription(createDTO.getDescription());
        todo.setUserId(userId);
        todo.setPriority(createDTO.getPriority());
        todo.setDueTime(createDTO.getDueTime());
        todo.setRemindTime(createDTO.getRemindTime());
        todo.setTodoType(createDTO.getType());
        todo.setCategory(createDTO.getCategory());
        todo.setSourceId(createDTO.getSourceId());

        // 如果是指派给他人的任务，设置assignUserId
        if (createDTO.getAssignUserId() != null) {
            todo.setAssignUserId(createDTO.getAssignUserId());
        }

        Todo savedTodo = todoRepository.save(todo);
        return convertToDTO(savedTodo);
    }

    // 创建系统自动待办
    public void createSystemTodo(Long userId, String title, String description,
                                 String priority, String sourceId, LocalDateTime dueTime) {
        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setDescription(description);
        todo.setUserId(userId);
        todo.setPriority(priority);
        todo.setTodoType("SYSTEM");
        todo.setSourceId(sourceId);
        todo.setDueTime(dueTime);

        todoRepository.save(todo);
    }

    // 更新待办状态
    @Transactional
    public TodoDTO updateTodoStatus(Long todoId, Boolean completed) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new RuntimeException("待办事项不存在"));

        todo.setCompleted(completed);
        if (completed) {
            todo.setCompleteTime(LocalDateTime.now());
        } else {
            todo.setCompleteTime(null);
        }

        Todo updatedTodo = todoRepository.save(todo);
        return convertToDTO(updatedTodo);
    }

    // 删除待办
    public ResponseEntity<?> deleteTodo(Long id,String token) {
        try {
            String actualToken = token.replace("Bearer ", "");
//            Long userId = authService.getUserIdByToken(actualToken);

            // 先检查待办是否存在且属于当前用户
            Todo todo = todoRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("待办事项不存在"));

            // 验证权限：只能删除自己的待办（除非是管理员）
//            if (!todo.getUserId().equals(userId)) {
//                // 可以添加管理员权限检查
//                // User currentUser = userRepository.findById(userId).orElseThrow();
//                // if (!"admin".equals(currentUser.getRoleCode()) && !"super".equals(currentUser.getRoleCode())) {
//                //     return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                //             .body(Map.of("success", false, "message", "无权删除他人的待办事项"));
//                // }
//                return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                        .body(Map.of("success", false, "message", "无权删除他人的待办事项"));
//            }

            todoRepository.deleteById(id);
            return ResponseEntity.ok()
                    .body(Map.of(
                            "success", true,
                            "message", "删除成功"
                    ));
        } catch (Exception e) {
            log.error("删除待办失败: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "删除失败: " + e.getMessage()
                    ));
        }

    }

    // 获取待办统计
    public Map<String, Object> getTodoStats(Long userId) {
        Long total = todoRepository.countByUserIdAndCompleted(userId, false);
        Long highPriority = (long) todoRepository.findByUserIdAndPriorityAndCompleted(
                userId, "HIGH", false).size();
        Long todayCount = (long) getTodayTodos(userId).size();

        return Map.of(
                "total", total,
                "highPriority", highPriority,
                "todayCount", todayCount
        );
    }

    // 转换实体为DTO
    private TodoDTO convertToDTO(Todo todo) {
        TodoDTO dto = new TodoDTO();
        dto.setId(todo.getId());
        dto.setTitle(todo.getTitle());
        dto.setDescription(todo.getDescription());
        dto.setCompleted(todo.getCompleted());
        dto.setPriority(todo.getPriority());
        dto.setPriorityLabel(getPriorityLabel(todo.getPriority()));
        dto.setDueTime(todo.getDueTime());
        dto.setRemindTime(todo.getRemindTime());
        dto.setTodoType(todo.getTodoType());
        dto.setTypeLabel(getTypeLabel(todo.getTodoType()));
        dto.setCategory(todo.getCategory());
        dto.setSourceId(todo.getSourceId());
        dto.setCreateTime(todo.getCreateTime());
        dto.setUpdateTime(todo.getUpdateTime());
        dto.setCompleteTime(todo.getCompleteTime());
        dto.setTimeLabel(formatTimeLabel(todo.getDueTime()));

        // 如果有被指派用户，获取用户名
        if (todo.getAssignUserId() != null) {
            User assignUser = userRepository.findById(todo.getAssignUserId()).orElse(null);
            if (assignUser != null) {
                dto.setAssignUserName(assignUser.getName());
            }
        }

        return dto;
    }

    private String getPriorityLabel(String priority) {
        return switch (priority) {
            case "HIGH" -> "高";
            case "MEDIUM" -> "中";
            case "LOW" -> "低";
            default -> "中";
        };
    }

    private String getTypeLabel(String type) {
        return switch (type) {
            case "PERSONAL" -> "个人";
            case "SYSTEM" -> "系统";
            case "ASSIGNED" -> "指派";
            default -> "个人";
        };
    }

    private String formatTimeLabel(LocalDateTime dueTime) {
        if (dueTime == null) {
            return "无期限";
        }

        LocalDate dueDate = dueTime.toLocalDate();
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        if (dueDate.equals(today)) {
            return "今天 " + dueTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else if (dueDate.equals(tomorrow)) {
            return "明天 " + dueTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else {
            return dueTime.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
        }
    }
}