import {
  BoldOutlined,
  ItalicOutlined,
  UnderlineOutlined,
  OrderedListOutlined,
  UnorderedListOutlined,
  LinkOutlined,
  PaperClipOutlined,
  EyeOutlined,
  EditOutlined
} from '@ant-design/icons';
import { Card, Button, Space, Tooltip, Divider, Upload, message } from 'antd';
import React, { useState, useRef } from 'react';

const QueryResponseEditor = ({ value, onChange, placeholder, disabled = false }) => {
  const [isPreview, setIsPreview] = useState(false);
  const editorRef = useRef(null);

  const formatText = (command, value = null) => {
    if (editorRef.current) {
      editorRef.current.focus();
      document.execCommand(command, false, value);
      // Trigger onChange to update parent component
      const content = editorRef.current.innerHTML;
      onChange && onChange(content);
    }
  };

  const handleInput = () => {
    if (editorRef.current) {
      const content = editorRef.current.innerHTML;
      onChange && onChange(content);
    }
  };

  const insertLink = () => {
    // eslint-disable-next-line no-alert
    const url = prompt('Enter URL:');
    if (url) {
      formatText('createLink', url);
    }
  };

  const insertList = (ordered = false) => {
    formatText(ordered ? 'insertOrderedList' : 'insertUnorderedList');
  };

  const handleFileUpload = file => {
    // In a real implementation, you would upload the file to a server
    // and insert a link or reference to it in the editor
    message.success(`File ${file.name} uploaded successfully`);

    // For demo purposes, just insert a file reference
    const fileRef = `[Attachment: ${file.name}]`;
    if (editorRef.current) {
      editorRef.current.focus();
      document.execCommand('insertText', false, fileRef);
      handleInput();
    }

    return false; // Prevent default upload behavior
  };

  const getPlainText = html => {
    const div = document.createElement('div');
    div.innerHTML = html;
    return div.textContent || div.innerText || '';
  };

  const renderPreview = () => {
    if (!value) {
      return <div style={{ color: '#999', fontStyle: 'italic' }}>No content to preview</div>;
    }

    return (
      <div
        style={{
          minHeight: '200px',
          padding: '12px',
          border: '1px solid #d9d9d9',
          borderRadius: '6px',
          backgroundColor: '#fafafa'
        }}
        dangerouslySetInnerHTML={{ __html: value }}
      />
    );
  };

  return (
    <Card size="small" className="query-response-editor" style={{ border: '1px solid #d9d9d9' }}>
      {/* Toolbar */}
      <div style={{ marginBottom: 8, borderBottom: '1px solid #f0f0f0', paddingBottom: 8 }}>
        <Space wrap>
          <Space.Compact>
            <Tooltip title="Bold">
              <Button
                size="small"
                icon={<BoldOutlined />}
                onClick={() => formatText('bold')}
                disabled={disabled || isPreview}
              />
            </Tooltip>
            <Tooltip title="Italic">
              <Button
                size="small"
                icon={<ItalicOutlined />}
                onClick={() => formatText('italic')}
                disabled={disabled || isPreview}
              />
            </Tooltip>
            <Tooltip title="Underline">
              <Button
                size="small"
                icon={<UnderlineOutlined />}
                onClick={() => formatText('underline')}
                disabled={disabled || isPreview}
              />
            </Tooltip>
          </Space.Compact>

          <Divider type="vertical" />

          <Space.Compact>
            <Tooltip title="Bulleted List">
              <Button
                size="small"
                icon={<UnorderedListOutlined />}
                onClick={() => insertList(false)}
                disabled={disabled || isPreview}
              />
            </Tooltip>
            <Tooltip title="Numbered List">
              <Button
                size="small"
                icon={<OrderedListOutlined />}
                onClick={() => insertList(true)}
                disabled={disabled || isPreview}
              />
            </Tooltip>
          </Space.Compact>

          <Divider type="vertical" />

          <Tooltip title="Insert Link">
            <Button
              size="small"
              icon={<LinkOutlined />}
              onClick={insertLink}
              disabled={disabled || isPreview}
            />
          </Tooltip>

          <Upload
            beforeUpload={handleFileUpload}
            showUploadList={false}
            disabled={disabled || isPreview}
          >
            <Tooltip title="Attach File">
              <Button size="small" icon={<PaperClipOutlined />} disabled={disabled || isPreview} />
            </Tooltip>
          </Upload>

          <Divider type="vertical" />

          <Button
            size="small"
            icon={isPreview ? <EditOutlined /> : <EyeOutlined />}
            onClick={() => setIsPreview(!isPreview)}
            type={isPreview ? 'primary' : 'default'}
          >
            {isPreview ? 'Edit' : 'Preview'}
          </Button>
        </Space>
      </div>

      {/* Editor/Preview Area */}
      {isPreview ? (
        renderPreview()
      ) : (
        <div
          ref={editorRef}
          contentEditable={!disabled}
          onInput={handleInput}
          style={{
            minHeight: '200px',
            padding: '12px',
            border: '1px solid #d9d9d9',
            borderRadius: '6px',
            outline: 'none',
            backgroundColor: disabled ? '#f5f5f5' : '#fff',
            color: disabled ? '#999' : '#000',
            direction: 'ltr',
            textAlign: 'left',
            fontFamily: 'inherit',
            fontSize: '14px',
            lineHeight: '1.5'
          }}
          dangerouslySetInnerHTML={{ __html: value || '' }}
          data-placeholder={placeholder}
        />
      )}

      {/* Character count */}
      <div
        style={{
          marginTop: 8,
          textAlign: 'right',
          fontSize: '12px',
          color: '#999'
        }}
      >
        {value ? getPlainText(value).length : 0} characters
      </div>

      <style
        dangerouslySetInnerHTML={{
          __html: `
          .query-response-editor [contenteditable]:empty:before {
            content: attr(data-placeholder);
            color: #999;
            font-style: italic;
          }
          
          .query-response-editor [contenteditable] ul, 
          .query-response-editor [contenteditable] ol {
            margin: 8px 0;
            padding-left: 20px;
          }
          
          .query-response-editor [contenteditable] li {
            margin: 4px 0;
          }
          
          .query-response-editor [contenteditable] a {
            color: #1890ff;
            text-decoration: underline;
          }
          
          .query-response-editor [contenteditable] strong {
            font-weight: bold;
          }
          
          .query-response-editor [contenteditable] em {
            font-style: italic;
          }
          
          .query-response-editor [contenteditable] u {
            text-decoration: underline;
          }
        `
        }}
      />
    </Card>
  );
};

export default QueryResponseEditor;
