package com.luomiblog.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SafePathValidator（路径遍历防护）单元测试：
 * 覆盖 BUGLIST BUG-001/BUG-002 的攻击向量与正常路径。
 */
class SafePathValidatorTest {

    @TempDir
    Path tempDir;

    // ==================== isSafeArticleFilename ====================

    @Test
    @DisplayName("正常文件名：字母数字中文连字符 + .md 后缀")
    void acceptsNormalFilenames() {
        assertTrue(SafePathValidator.isSafeArticleFilename("hello-world.md"));
        assertTrue(SafePathValidator.isSafeArticleFilename("我的文章_2024.md"));
        assertTrue(SafePathValidator.isSafeArticleFilename("Vue3_Composition-API.md"));
    }

    @Test
    @DisplayName("路径遍历向量全部拒绝")
    void rejectsTraversalVectors() {
        assertFalse(SafePathValidator.isSafeArticleFilename("../../etc/passwd.md"));
        assertFalse(SafePathValidator.isSafeArticleFilename("..\\..\\evil.md"));
        assertFalse(SafePathValidator.isSafeArticleFilename("a/../b.md"));
        assertFalse(SafePathValidator.isSafeArticleFilename("foo/../../bar.md"));
        assertFalse(SafePathValidator.isSafeArticleFilename("/etc/passwd.md"));
        assertFalse(SafePathValidator.isSafeArticleFilename("C:\\Windows\\evil.md"));
        assertFalse(SafePathValidator.isSafeArticleFilename("..md"));
        assertFalse(SafePathValidator.isSafeArticleFilename(".md"));
    }

    @Test
    @DisplayName("非 .md 后缀与空值拒绝")
    void rejectsNonMdAndBlank() {
        assertFalse(SafePathValidator.isSafeArticleFilename("script.sh"));
        assertFalse(SafePathValidator.isSafeArticleFilename("config.yml"));
        assertFalse(SafePathValidator.isSafeArticleFilename("evil.md.exe"));
        assertFalse(SafePathValidator.isSafeArticleFilename("a.md.md"));
        assertFalse(SafePathValidator.isSafeArticleFilename(null));
        assertFalse(SafePathValidator.isSafeArticleFilename(""));
        assertFalse(SafePathValidator.isSafeArticleFilename("   "));
    }

    // ==================== isSafeSlug ====================

    @Test
    @DisplayName("正常 slug：字母数字中文下划线连字符")
    void acceptsNormalSlugs() {
        assertTrue(SafePathValidator.isSafeSlug("hello-world"));
        assertTrue(SafePathValidator.isSafeSlug("我的文章"));
        assertTrue(SafePathValidator.isSafeSlug("spring_boot_3_2"));
        assertTrue(SafePathValidator.isSafeSlug("a"));
    }

    @Test
    @DisplayName("slug 路径遍历与危险值拒绝")
    void rejectsDangerousSlugs() {
        assertFalse(SafePathValidator.isSafeSlug("../../evil"));
        assertFalse(SafePathValidator.isSafeSlug("..\\evil"));
        assertFalse(SafePathValidator.isSafeSlug(".."));
        assertFalse(SafePathValidator.isSafeSlug("."));
        assertFalse(SafePathValidator.isSafeSlug("a/b"));
        assertFalse(SafePathValidator.isSafeSlug("a\\b"));
        assertFalse(SafePathValidator.isSafeSlug(null));
        assertFalse(SafePathValidator.isSafeSlug(""));
        assertFalse(SafePathValidator.isSafeSlug("has space"));
        assertFalse(SafePathValidator.isSafeSlug("semi;colon"));
    }

    // ==================== isInsideRoot ====================

    @Test
    @DisplayName("目录树内路径放行")
    void acceptsInsideRoot() {
        Path inside = tempDir.resolve("sub").resolve("a.md");
        assertTrue(SafePathValidator.isInsideRoot(tempDir, inside));
        assertTrue(SafePathValidator.isInsideRoot(tempDir, tempDir.resolve("b.md")));
    }

    @Test
    @DisplayName("目录树外路径拒绝（含遍历后归一化）")
    void rejectsOutsideRoot() {
        Path outside = tempDir.resolve("..").resolve("escaped.md");
        assertFalse(SafePathValidator.isInsideRoot(tempDir, outside));
        Path sibling = tempDir.getParent().resolve("other.md");
        assertFalse(SafePathValidator.isInsideRoot(tempDir, sibling));
    }

    @Test
    @DisplayName("root 自身视为合法边界")
    void rootItselfIsInside() {
        assertTrue(SafePathValidator.isInsideRoot(tempDir, tempDir));
    }

    @Test
    @DisplayName("null 参数拒绝")
    void rejectsNull() {
        assertFalse(SafePathValidator.isInsideRoot(null, tempDir));
        assertFalse(SafePathValidator.isInsideRoot(tempDir, null));
    }
}
