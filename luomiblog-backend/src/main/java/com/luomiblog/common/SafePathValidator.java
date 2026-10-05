package com.luomiblog.common;

import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * 文件系统路径安全校验（防路径遍历）：
 * 与 SafeUrlValidator 同一风格——纯静态、返回布尔，由调用方决定映射为何种业务异常。
 * 范式对齐姊妹项目 AstrNest 的 StoragePathGuard（字符白名单 + resolve/normalize/startsWith 双重防护）。
 */
public final class SafePathValidator {

    /** 文章文件名：仅字母/数字/中文/下划线/连字符 + 必须以 .md 结尾（拒绝任何路径分隔符与第二个点） */
    private static final Pattern SAFE_ARTICLE_FILENAME = Pattern.compile("^[A-Za-z0-9\\u4e00-\\u9fa5_-]{1,120}\\.md$");

    /** 文章 slug：与 ArticleServiceImpl#generateSlug 的产出字符集一致（小写字母/数字/中文/下划线/连字符） */
    private static final Pattern SAFE_SLUG = Pattern.compile("^[A-Za-z0-9\\u4e00-\\u9fa5_-]{1,120}$");

    private SafePathValidator() {
    }

    /**
     * 上传接口的文章文件名校验：非空、白名单字符、单一 .md 后缀。
     * 拒绝 ../../、绝对路径、NUL、反斜杠与一切路径分隔符。
     */
    public static boolean isSafeArticleFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return false;
        }
        return SAFE_ARTICLE_FILENAME.matcher(filename).matches();
    }

    /**
     * slug 校验：用于拼文件路径（slug + ".md"）与 URL，禁止路径分隔符/点/空白。
     */
    public static boolean isSafeSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return false;
        }
        if (".".equals(slug) || "..".equals(slug)) {
            return false;
        }
        return SAFE_SLUG.matcher(slug).matches();
    }

    /**
     * 归一化后校验 candidate 必须落在 root 目录树内。
     * 防御纵深：即使字符白名单被绕过（或未来新增调用点），路径逃逸在此兜底。
     */
    public static boolean isInsideRoot(Path root, Path candidate) {
        if (root == null || candidate == null) {
            return false;
        }
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        return normalizedCandidate.startsWith(normalizedRoot);
    }
}
