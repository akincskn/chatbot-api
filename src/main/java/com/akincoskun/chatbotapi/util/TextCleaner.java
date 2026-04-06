package com.akincoskun.chatbotapi.util;

/**
 * Ham metni embedding ve display için temizler.
 */
public final class TextCleaner {

    private TextCleaner() {}

    /**
     * Gereksiz whitespace, kontrol karakterleri ve tekrarlanan boş satırları temizler.
     *
     * @param text ham metin
     * @return temizlenmiş metin
     */
    public static String clean(String text) {
        if (text == null || text.isBlank()) return "";

        return text
                // Unicode kontrol karakterlerini kaldır (null byte vb.)
                .replaceAll("[\\p{Cc}&&[^\n\t]]", "")
                // Satır içi boşlukları tek boşluğa indir
                .replaceAll("[ \t]+", " ")
                // 3+ ardışık newline'ı 2'ye indir
                .replaceAll("\n{3,}", "\n\n")
                // Satır başı/sonu boşlukları temizle
                .lines()
                .map(String::strip)
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b)
                .strip();
    }

    /**
     * Metindeki tahmini token sayısını hesaplar (yaklaşık: karakter / 4).
     *
     * @param text metin
     * @return tahmini token sayısı
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isBlank()) return 0;
        return Math.max(1, text.length() / 4);
    }
}
