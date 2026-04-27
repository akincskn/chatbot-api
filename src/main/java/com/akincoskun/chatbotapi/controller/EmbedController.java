package com.akincoskun.chatbotapi.controller;

import com.akincoskun.chatbotapi.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Embed script endpoint'leri. JWT gerektirmez.
 * GET /embed.js  — JavaScript widget dosyasını döner
 * GET /api/chatbots/:id/embed — HTML embed snippet'ı döner
 */
@RestController
@RequiredArgsConstructor
public class EmbedController {

    private final ChatbotService chatbotService;

    @Value("${app.api-base-url}")
    private String apiBaseUrl;

    /**
     * Embed widget JavaScript dosyasını döner.
     * data-chatbot-id attribute'ünden chatbot ID'yi okur.
     *
     * @return JavaScript widget kodu
     */
    @GetMapping(value = "/embed.js", produces = "application/javascript")
    public ResponseEntity<String> getEmbedScript() {
        String js = buildEmbedScript();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/javascript"))
                .header("Cache-Control", "public, max-age=3600")
                .body(js);
    }

    /**
     * Chatbot için HTML embed snippet'ı döner.
     *
     * @param id chatbot UUID
     * @return script tag snippet
     */
    @GetMapping("/api/chatbots/{id}/embed")
    public ResponseEntity<Map<String, String>> getEmbedSnippet(@PathVariable UUID id) {
        chatbotService.assertExists(id);
        String snippet = String.format(
                "<script src=\"%s/embed.js\" data-chatbot-id=\"%s\"></script>",
                apiBaseUrl, id);
        return ResponseEntity.ok(Map.of("snippet", snippet, "chatbotId", id.toString()));
    }

    private String buildEmbedScript() {
        return """
                (function () {
                  'use strict';

                  var script = document.currentScript ||
                    document.querySelector('script[data-chatbot-id]');
                  var CHATBOT_ID = script && script.getAttribute('data-chatbot-id');
                  if (!CHATBOT_ID) { console.error('[ChatBot] data-chatbot-id is missing'); return; }

                  var API_BASE = '%s';
                  var SESSION_KEY = 'cb_session_' + CHATBOT_ID;
                  var sessionId = localStorage.getItem(SESSION_KEY);
                  if (!sessionId) {
                    sessionId = ([1e7]+-1e3+-4e3+-8e3+-1e11).replace(/[018]/g, function(c) {
                      return (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16);
                    });
                    localStorage.setItem(SESSION_KEY, sessionId);
                  }

                  /* --- Styles --- */
                  var style = document.createElement('style');
                  style.textContent = [
                    '#cb-bubble{position:fixed;bottom:24px;right:24px;width:56px;height:56px;',
                    'border-radius:50%;background:#3B82F6;color:#fff;font-size:24px;',
                    'border:none;cursor:pointer;box-shadow:0 4px 12px rgba(0,0,0,.2);z-index:9999;}',
                    '#cb-window{position:fixed;bottom:96px;right:24px;width:360px;height:520px;',
                    'background:#fff;border-radius:12px;box-shadow:0 8px 30px rgba(0,0,0,.15);',
                    'display:none;flex-direction:column;z-index:9998;font-family:sans-serif;}',
                    '#cb-window.open{display:flex;}',
                    '#cb-header{background:#3B82F6;color:#fff;padding:16px;border-radius:12px 12px 0 0;',
                    'font-weight:600;font-size:15px;}',
                    '#cb-messages{flex:1;overflow-y:auto;padding:16px;display:flex;flex-direction:column;gap:10px;}',
                    '.cb-msg{max-width:80%;padding:10px 14px;border-radius:16px;font-size:14px;line-height:1.4;}',
                    '.cb-msg.user{background:#3B82F6;color:#fff;align-self:flex-end;border-bottom-right-radius:4px;}',
                    '.cb-msg.bot{background:#F3F4F6;color:#1F2937;align-self:flex-start;border-bottom-left-radius:4px;}',
                    '#cb-form{display:flex;padding:12px;gap:8px;border-top:1px solid #E5E7EB;}',
                    '#cb-input{flex:1;border:1px solid #D1D5DB;border-radius:8px;padding:8px 12px;',
                    'font-size:14px;outline:none;}',
                    '#cb-send{background:#3B82F6;color:#fff;border:none;border-radius:8px;',
                    'padding:8px 14px;cursor:pointer;font-size:14px;}'
                  ].join('');
                  document.head.appendChild(style);

                  /* --- Widget HTML --- */
                  var bubble = document.createElement('button');
                  bubble.id = 'cb-bubble';
                  bubble.innerHTML = '&#128172;';
                  bubble.title = 'Chat';

                  var win = document.createElement('div');
                  win.id = 'cb-window';
                  win.innerHTML = [
                    '<div id="cb-header">Chat</div>',
                    '<div id="cb-messages"></div>',
                    '<form id="cb-form">',
                    '<input id="cb-input" type="text" placeholder="Type a message..." autocomplete="off"/>',
                    '<button id="cb-send" type="submit">Send</button>',
                    '</form>'
                  ].join('');

                  document.body.appendChild(bubble);
                  document.body.appendChild(win);

                  var messages = win.querySelector('#cb-messages');
                  var form = win.querySelector('#cb-form');
                  var input = win.querySelector('#cb-input');

                  bubble.addEventListener('click', function () {
                    win.classList.toggle('open');
                    if (win.classList.contains('open')) input.focus();
                  });

                  function appendMessage(text, role) {
                    var div = document.createElement('div');
                    div.className = 'cb-msg ' + role;
                    div.textContent = text;
                    messages.appendChild(div);
                    messages.scrollTop = messages.scrollHeight;
                  }

                  function setLoading(on) {
                    var btn = win.querySelector('#cb-send');
                    btn.disabled = on;
                    btn.textContent = on ? '...' : 'Send';
                  }

                  form.addEventListener('submit', function (e) {
                    e.preventDefault();
                    var text = input.value.trim();
                    if (!text) return;
                    input.value = '';
                    appendMessage(text, 'user');
                    setLoading(true);

                    fetch(API_BASE + '/api/chat/public/' + CHATBOT_ID, {
                      method: 'POST',
                      headers: { 'Content-Type': 'application/json' },
                      body: JSON.stringify({ message: text, sessionId: sessionId })
                    })
                    .then(function(r) { return r.json(); })
                    .then(function(data) {
                      appendMessage(data.message || 'Sorry, I could not respond.', 'bot');
                    })
                    .catch(function() {
                      appendMessage('Connection error. Please try again.', 'bot');
                    })
                    .finally(function() { setLoading(false); });
                  });
                })();
                """.formatted(apiBaseUrl);
    }
}
