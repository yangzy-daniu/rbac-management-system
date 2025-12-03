package com.example.demo.controller;

import com.example.demo.annotation.OperationLog;
import com.example.demo.dto.TodoCreateDTO;
import com.example.demo.dto.TodoDTO;
import com.example.demo.service.AuthService;
import com.example.demo.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;
    private final AuthService authService;

    // 获取待办列表
    @GetMapping
    public List<TodoDTO> getTodos(
            @RequestParam(required = false, defaultValue = "false") Boolean completed,
            @RequestParam(required = false) Integer limit,
            @RequestHeader("Authorization") String token) {
        String actualToken = token.replace("Bearer ", "");
        Long userId = authService.getUserIdByToken(actualToken);

        return todoService.getUserTodos(userId, completed, limit);
    }

    // 获取今日待办
    @GetMapping("/today")
    public List<TodoDTO> getTodayTodos(@RequestHeader("Authorization") String token) {
        String actualToken = token.replace("Bearer ", "");
        Long userId = authService.getUserIdByToken(actualToken);

        return todoService.getTodayTodos(userId);
    }

    // 获取待办统计
    @GetMapping("/stats")
    public Map<String, Object> getTodoStats(@RequestHeader("Authorization") String token) {
        String actualToken = token.replace("Bearer ", "");
        Long userId = authService.getUserIdByToken(actualToken);

        return todoService.getTodoStats(userId);
    }

    // 创建待办
    @OperationLog(module = "待办事项", type = "CREATE", value = "创建待办")
    @PostMapping
    public TodoDTO createTodo(@RequestBody TodoCreateDTO createDTO,
                              @RequestHeader("Authorization") String token) {
        String actualToken = token.replace("Bearer ", "");
        Long userId = authService.getUserIdByToken(actualToken);

        return todoService.createTodo(userId, createDTO);
    }

    // 更新待办状态
    @OperationLog(module = "待办事项", type = "UPDATE", value = "更新待办状态")
    @PutMapping("/{id}/status")
    public TodoDTO updateTodoStatus(@PathVariable Long id,
                                    @RequestParam Boolean completed,
                                    @RequestHeader("Authorization") String token) {
        return todoService.updateTodoStatus(id, completed);
    }

    // 删除待办
    @OperationLog(module = "待办事项", type = "DELETE", value = "删除待办")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTodo(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        return todoService.deleteTodo(id,token);
    }
}