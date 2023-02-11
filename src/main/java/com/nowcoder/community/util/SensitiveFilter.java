package com.nowcoder.community.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class SensitiveFilter {

    /**
     * 替换符
     */
    private static final String REPLACEMENT = "***";

    /**
     * 根节点
     */
    private TrieNode rootNode = new TrieNode();

    /**
     * 初始化方法，在创建容器后初始化
     */
    @PostConstruct
    public void init() {
        try (
                // 通过类加载器来获取resource
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                // 转换为BufferedReader，提高效率
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        ) {
            String keyword;
            while ((keyword = reader.readLine()) != null) {
                // 添加到前缀树
                this.addKeyWord(keyword);
            }

        } catch (IOException e) {
            log.debug("加载敏感词文件失败：" + e.getMessage());
        }
    }

    /**
     * 将一个敏感词添加在前缀树中
     * @param keyword 敏感词
     */
    private void addKeyWord(String keyword) {
        TrieNode tempNode = rootNode;
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);

            if (subNode == null) {
                // 初始化子节点
                subNode = new TrieNode();
                tempNode.addSubNode(c, subNode);
            }

            // 指向下一层，也就是子节点，进入下一层循环
            tempNode = subNode;
            // 设置结束的标识
            if (i == keyword.length() - 1) {
                tempNode.setKeyWordEnd(true);
            }
        }
    }

    /**
     * 过滤敏感词
     *
     * @param text 待过滤的文本
     * @return 过滤后的文本
     */
    public String filter(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }

        // 指针1 指向根节点
        TrieNode tempNode = rootNode;
        // 指针2 指向文本开头
        int begin = 0;
        // 指针3 指向敏感词的结尾
        int position = 0;
        // 结果
        StringBuilder res = new StringBuilder();

        while (begin < text.length()) { // 第二个指针遍历
            char c = text.charAt(begin);
            tempNode = tempNode.getSubNode(c);
            if (tempNode != null) { // 第三个指针遍历查找是否匹配敏感词
                position = begin + 1;
                while (tempNode != null && !tempNode.isKeyWordEnd() && position < text.length()) {
                    char tmp = text.charAt(position);
                    if (isSymbol(tmp)) { // 是否为符号
                        res.append(tmp);
                        position++;
                        continue;
                    }
                    tempNode = tempNode.getSubNode(tmp); // 遍历下一层
                    position++;
                }
                if (tempNode != null && tempNode.isKeyWordEnd) {
                    res.append(REPLACEMENT);
                    begin = position;
                    tempNode = rootNode;
                    continue;
                }
            }
            res.append(c);
            begin++;
            tempNode = rootNode;
        }
        return res.toString();
    }

    // 判断是否为符号
    private boolean isSymbol(Character c) {
        // 0x2E80~OX9FF 是东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    /**
     * 前缀树节点
     */
    private class TrieNode {

        /**
         * 关键词结束的标志
         */
        private  boolean isKeyWordEnd = false;

        /**
         * 子节点<p></p>
         * key是下级字符，value是下级节点
         */
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isKeyWordEnd() {
            return isKeyWordEnd;
        }

        public void setKeyWordEnd(boolean keyWordEnd) {
            isKeyWordEnd = keyWordEnd;
        }

        /**
         * 添加子节点
         */
        public void addSubNode(Character c, TrieNode node) {
            subNodes.put(c, node);
        }

        /**
         * 获取子节点
         */
        public TrieNode getSubNode(Character c) {
            return subNodes.get(c);
        }

    }

}
