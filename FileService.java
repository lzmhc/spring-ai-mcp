package com.lzmhc.mcpserver;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class FileService {
    private static String MCP_FILE_DIRECTORY_PATH = System.getenv("MCP_FILE_DIRECTORY_PATH").endsWith("/")?System.getenv("MCP_FILE_DIRECTORY_PATH"):System.getenv("MCP_FILE_DIRECTORY_PATH")+"/";
    @Tool(description = "Read the complete contents of a file from the file system. " +
            "Handles various text encodings and provides detailed error messages " +
            "if the file cannot be read. Use this tool when you need to examine " +
            "the contents of a single file. Only works within allowed directories.")
    public String read_file(@ToolParam(description = "文件名称")String directoryPath){
        try {
            // 1. 安全路径验证
            Path validatedPath = validatePath(MCP_FILE_DIRECTORY_PATH+directoryPath);
            log.info("读取文件{}",validatedPath.getFileName());
            // 2. 读取文件内容（自动检测编码）
            return readFileWithEncodingDetection(validatedPath);
        } catch (InvalidPathException e) {
            return "Error: Invalid path format - " + e.getReason();
        } catch (SecurityException e) {
            return "Error: Access denied - " + e.getMessage();
        } catch (NoSuchFileException e) {
            return "Error: File not found - " + e.getFile();
        } catch (MalformedInputException e) {
            return "Error: Encoding mismatch - File is not UTF-8 encoded";
        } catch (IOException e) {
            return "Error: Failed to read file - " + e.getMessage();
        }
    }
    @Tool(description = "\"Read the contents of multiple files simultaneously. This is more \" +\n" +
            "          \"efficient than reading files one by one when you need to analyze \" +\n" +
            "          \"or compare multiple files. Each file's content is returned with its \" +\n" +
            "          \"path as a reference. Failed reads for individual files won't stop \" +\n" +
            "          \"the entire operation. Only works within allowed directories.\"")
    public String read_multiple_files(){
        return "hello world!";
    }
    @Tool(description = "Create a new file or completely overwrite an existing file with new content. " +
            "Use with caution as it will overwrite existing files without warning. " +
            "Handles text content with proper encoding. Only works within allowed directories.")
    public String write_file(@ToolParam(description = "文件名称") String fileName ,@ToolParam(description = "文件内容") String content){
        try {
            // 1. 内容大小校验

            // 2. 安全路径验证
            Path validatedPath = validatePath(MCP_FILE_DIRECTORY_PATH+fileName);

            // 3. 创建父目录（如果需要）

            // 4. 写入文件内容
            Files.write(validatedPath,
                    content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            return "Successfully wrote to: " + MCP_FILE_DIRECTORY_PATH;
        } catch (InvalidPathException e) {
            return "Error: Invalid path format - " + e.getReason();
        } catch (SecurityException e) {
            return "Error: Access denied - " + e.getMessage();
        } catch (IOException e) {
            return "Error: File operation failed - " + e.getMessage();
        }
    }
    @Tool(description =  "Make line-based edits to a text file. Each edit replaces exact line sequences " +
            "with new content. Returns a git-style diff showing the changes made. " +
            "Only works within allowed directories.")
    public String edit_file(){
        return "update";
    }
    @Tool(description =  "Create a new directory or ensure a directory exists. Can create multiple " +
            "nested directories in one operation. If the directory already exists, " +
            "this operation will succeed silently. Perfect for setting up directory " +
            "structures for projects or ensuring required paths exist. Only works within allowed directories.")
    public String create_directory(@ToolParam(description = "新目录名称")String directoryPath){
        try {
            // 1. 路径安全验证
            Path validatedPath = validatePath(MCP_FILE_DIRECTORY_PATH+directoryPath);
            log.info("创建目录{}", validatedPath.toAbsolutePath());
            // 2. 检查是否已存在
            if (Files.exists(validatedPath)) {
                if (Files.isDirectory(validatedPath)) {
                    return "Directory already exists: " + directoryPath;
                }
                return "Error: Path exists but is not a directory";
            }

            // 3. 创建目录（包含父目录）
            Files.createDirectories(validatedPath);

            return "Successfully created directory: " + directoryPath;
        } catch (InvalidPathException e) {
            log.error(e.getMessage());
            return "Error: Invalid path format - " + e.getReason();
        } catch (SecurityException e) {
            log.error(e.getMessage());
            return "Error: Access denied - " + e.getMessage();
        } catch (IOException e) {
            log.error(e.getMessage());
            return "Error: Failed to create directory - " + e.getMessage();
        }
    }
    @Tool(description = " \"Get a detailed listing of all files and directories in a specified path. \" +\n" +
            "          \"Results clearly distinguish between files and directories with [FILE] and [DIR] \" +\n" +
            "          \"prefixes. This tool is essential for understanding directory structure and \" +\n" +
            "          \"finding specific files within a directory. Only works within allowed directories.\",")
    public List<String> list_directory(){
        try {
            // 1. 路径安全验证
            Path validatedPath = validatePath(MCP_FILE_DIRECTORY_PATH);

            // 2. 验证是否为目录
            if (!Files.isDirectory(validatedPath)) {
                return List.of("Error: Specified path is not a directory");
            }

            // 3. 获取目录内容
            try (Stream<Path> paths = Files.list(validatedPath)) {
                return paths
                        .map(this::formatEntry)
                        .collect(Collectors.toList());
            }
        } catch (InvalidPathException e) {
            return List.of("Error: Invalid path format - " + e.getMessage());
        } catch (SecurityException e) {
            return List.of("Error: Access denied - " + e.getMessage());
        } catch (IOException e) {
            return List.of("Error: Failed to read directory - " + e.getMessage());
        }
    }
    @Tool(description = "Get a recursive tree view of files and directories as a JSON structure. " +
            "Each entry includes 'name', 'type' (file/directory), and 'children' for directories. " +
            "Files have no children array, while directories always have a children array (which may be empty). " +
            "The output is formatted with 2-space indentation for readability. Only works within allowed directories.")
    public String directory_tree(){
        return "目录树";
    }
    @Tool(description = "Move or rename files and directories. Can move files between directories " +
            "and rename them in a single operation. If the destination exists, the " +
            "operation will fail. Works across different directories and can be used " +
            "for simple renaming within the same directory. Both source and destination must be within allowed directories.")
    public String move_file(){
        return "移动文件";
    }
    @Tool(description = "Recursively search for files and directories matching a pattern. " +
            "Searches through all subdirectories from the starting path. The search " +
            "is case-insensitive and matches partial names. Returns full paths to all " +
            "matching items. Great for finding files when you don't know their exact location. " +
            "Only searches within allowed directories.")
    public String search_files(){
        return "查询文件";
    }
    @Tool(description =  "Retrieve detailed metadata about a file or directory. Returns comprehensive " +
            "information including size, creation time, last modified time, permissions, " +
            "and type. This tool is perfect for understanding file characteristics " +
            "without reading the actual content. Only works within allowed directories.")
    public String get_file_info(){
        return "获取文件信息";
    }
    @Tool(description =  "Returns the list of directories that this server is allowed to access. " +
            "Use this to understand which directories are available before trying to access files.")
    public String list_allowed_directories(){
        if(MCP_FILE_DIRECTORY_PATH.isEmpty() || MCP_FILE_DIRECTORY_PATH.equals("")){
            return "您还没有设置MCP_FILE_DIRECTORY_PATH环境变量，ai无法修改系统配置文件";
        }else{
            return MCP_FILE_DIRECTORY_PATH;
        }
    }

    /**
     * 格式化目录条目
     */
    private String formatEntry(Path path) {
        try {
            // 二次验证路径安全性
            Path realPath = path.toRealPath();
            boolean isDir = Files.isDirectory(realPath);

            // 获取基础信息
            String size = isDir ? "" : " Size: " + formatSize(Files.size(realPath));
            return String.format("[%s] %s%s",
                    isDir ? "DIR" : "FILE",
                    path.getFileName(),
                    size);
        } catch (IOException e) {
            return "[ERROR] " + path.getFileName() + " (Unreadable)";
        }
    }

    /**
     * 安全路径验证（复用之前实现的验证逻辑）
     */
    private Path validatePath(String userInputPath) throws SecurityException {
        try {
            // 解析路径
            Path resolvedPath = Paths.get(userInputPath)
                    .normalize()
                    .toAbsolutePath();

            // 检查是否在允许目录内
            Path allowed = Paths.get(MCP_FILE_DIRECTORY_PATH).normalize();
            if (!resolvedPath.startsWith(allowed)) {
                throw new SecurityException("Path outside allowed directory");
            }

            return resolvedPath;
        } catch (InvalidPathException e) {
            throw new SecurityException("Invalid path: " + e.getMessage());
        }
    }

    /**
     * 格式化文件大小
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String unit = "KMGTPE".charAt(exp-1) + "iB";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), unit);
    }
    /**
     * 支持编码检测的文件读取
     */
    private String readFileWithEncodingDetection(Path filePath) throws IOException {
        try {
            // 优先尝试UTF-8
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            // 回退到系统默认编码
            return Files.readString(filePath, Charset.defaultCharset());
        }
    }
}
