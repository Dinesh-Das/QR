import {
  DashboardOutlined,
  FileTextOutlined,
  TeamOutlined,
  RocketOutlined,
  SafetyOutlined,
  AuditOutlined
} from '@ant-design/icons';
import {
  Card,
  Row,
  Col,
  Typography
} from 'antd';
import React from 'react';

const { Title, Paragraph, Text } = Typography;

const Home = () => (
  <div style={{ padding: 0, background: 'transparent', minHeight: 'calc(100vh - 100px)' }}>
    {/* Hero Section */}
    <div
      style={{
        background: 'linear-gradient(135deg, #1890ff 0%, #096dd9 100%)',
        borderRadius: 12,
        padding: '48px 32px',
        marginBottom: 24,
        color: 'white',
        textAlign: 'center'
      }}
    >
      <RocketOutlined style={{ fontSize: 48, marginBottom: 16 }} />
      <Title level={1} style={{ color: 'white', marginBottom: 16 }}>
        [QR][MFG][012] - EVALUATION OF RM BASED ON INTRINSIC HAZARDS, IDENTIFICATION OF GAPS AND CLOSURE OF GAPS
      </Title>
      <Paragraph
        style={{ color: 'rgba(255,255,255,0.9)', fontSize: 18, maxWidth: 800, margin: '0 auto' }}
      >
        A comprehensive workflow management system designed to streamline quality risk assessments,
        document management, and compliance tracking across manufacturing operations.
      </Paragraph>
    </div>


    {/* Main Features */}
    <Row gutter={[24, 24]} style={{ marginBottom: 32 }}>
      <Col xs={24} md={12} lg={6}>
        <Card 
          hoverable 
          style={{ 
            textAlign: 'center', 
            height: '100%',
            borderRadius: '12px',
            background: 'linear-gradient(145deg, #ffffff 0%, #f8fafc 100%)',
            border: '1px solid #e2e8f0',
            transition: 'all 0.3s ease'
          }}
          bodyStyle={{ padding: '32px 24px' }}
        >
          <div style={{
            background: 'linear-gradient(135deg, #1890ff 0%, #40a9ff 100%)',
            borderRadius: '50%',
            width: '80px',
            height: '80px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 20px auto',
            boxShadow: '0 8px 24px rgba(24, 144, 255, 0.3)'
          }}>
            <DashboardOutlined style={{ fontSize: 36, color: 'white' }} />
          </div>
          <Title level={4} style={{ color: '#1f2937', marginBottom: 12 }}>Workflow Management</Title>
          <Paragraph style={{ color: '#6b7280', fontSize: '14px', lineHeight: '1.6' }}>
            Track and manage quality risk workflows from initiation to completion across JVC,
            Plant, CQS, and Tech teams.
          </Paragraph>
        </Card>
      </Col>
      <Col xs={24} md={12} lg={6}>
        <Card 
          hoverable 
          style={{ 
            textAlign: 'center', 
            height: '100%',
            borderRadius: '12px',
            background: 'linear-gradient(145deg, #ffffff 0%, #f8fafc 100%)',
            border: '1px solid #e2e8f0',
            transition: 'all 0.3s ease'
          }}
          bodyStyle={{ padding: '32px 24px' }}
        >
          <div style={{
            background: 'linear-gradient(135deg, #52c41a 0%, #73d13d 100%)',
            borderRadius: '50%',
            width: '80px',
            height: '80px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 20px auto',
            boxShadow: '0 8px 24px rgba(82, 196, 26, 0.3)'
          }}>
            <FileTextOutlined style={{ fontSize: 36, color: 'white' }} />
          </div>
          <Title level={4} style={{ color: '#1f2937', marginBottom: 12 }}>Document Control</Title>
          <Paragraph style={{ color: '#6b7280', fontSize: '14px', lineHeight: '1.6' }}>
            Centralized document management with version control, access tracking, and secure
            storage.
          </Paragraph>
        </Card>
      </Col>
      <Col xs={24} md={12} lg={6}>
        <Card 
          hoverable 
          style={{ 
            textAlign: 'center', 
            height: '100%',
            borderRadius: '12px',
            background: 'linear-gradient(145deg, #ffffff 0%, #f8fafc 100%)',
            border: '1px solid #e2e8f0',
            transition: 'all 0.3s ease'
          }}
          bodyStyle={{ padding: '32px 24px' }}
        >
          <div style={{
            background: 'linear-gradient(135deg, #722ed1 0%, #9254de 100%)',
            borderRadius: '50%',
            width: '80px',
            height: '80px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 20px auto',
            boxShadow: '0 8px 24px rgba(114, 46, 209, 0.3)'
          }}>
            <AuditOutlined style={{ fontSize: 36, color: 'white' }} />
          </div>
          <Title level={4} style={{ color: '#1f2937', marginBottom: 12 }}>Query Management</Title>
          <Paragraph style={{ color: '#6b7280', fontSize: '14px', lineHeight: '1.6' }}>
            Streamlined query resolution system with team assignments and SLA tracking.
          </Paragraph>
        </Card>
      </Col>
      <Col xs={24} md={12} lg={6}>
        <Card 
          hoverable 
          style={{ 
            textAlign: 'center', 
            height: '100%',
            borderRadius: '12px',
            background: 'linear-gradient(145deg, #ffffff 0%, #f8fafc 100%)',
            border: '1px solid #e2e8f0',
            transition: 'all 0.3s ease'
          }}
          bodyStyle={{ padding: '32px 24px' }}
        >
          <div style={{
            background: 'linear-gradient(135deg, #fa8c16 0%, #ffa940 100%)',
            borderRadius: '50%',
            width: '80px',
            height: '80px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 20px auto',
            boxShadow: '0 8px 24px rgba(250, 140, 22, 0.3)'
          }}>
            <TeamOutlined style={{ fontSize: 36, color: 'white' }} />
          </div>
          <Title level={4} style={{ color: '#1f2937', marginBottom: 12 }}>Role-Based Access</Title>
          <Paragraph style={{ color: '#6b7280', fontSize: '14px', lineHeight: '1.6' }}>
            Comprehensive RBAC system ensuring secure access control and audit compliance.
          </Paragraph>
        </Card>
      </Col>
    </Row>

    {/* About Section */}
    <Card
      style={{ 
        marginBottom: 24,
        borderRadius: '12px',
        background: 'linear-gradient(145deg, #ffffff 0%, #f8fafc 100%)',
        border: '1px solid #e2e8f0',
        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.05)'
      }}
      bodyStyle={{ padding: '32px' }}
    >
      <div style={{ 
        display: 'flex', 
        alignItems: 'center', 
        marginBottom: 24,
        paddingBottom: 16,
        borderBottom: '1px solid #e2e8f0'
      }}>
        <div style={{
          background: 'linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%)',
          borderRadius: '50%',
          width: '48px',
          height: '48px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginRight: 16,
          boxShadow: '0 4px 12px rgba(245, 158, 11, 0.3)'
        }}>
          <SafetyOutlined style={{ fontSize: 24, color: 'white' }} />
        </div>
        <Title level={3} style={{ margin: 0, color: '#1f2937' }}>About QRMFG</Title>
      </div>
      
      <Row gutter={[32, 24]}>
        <Col xs={24} lg={12}>
          <Paragraph style={{ fontSize: '16px', lineHeight: '1.7', color: '#374151' }}>
            <Text strong style={{ color: '#1f2937' }}>QRMFG (Quality Risk Management for Manufacturing)</Text> is an
            enterprise-grade workflow management system designed specifically for manufacturing
            quality assurance processes.
          </Paragraph>
        </Col>
        <Col xs={24} lg={12}>
          <Title level={5} style={{ color: '#1f2937', marginBottom: 16 }}>Key Capabilities:</Title>
          <ul style={{ paddingLeft: 0, lineHeight: '1.8', listStyle: 'none' }}>
            <li style={{ marginBottom: 12, display: 'flex', alignItems: 'flex-start' }}>
              <span style={{ 
                background: '#1890ff', 
                borderRadius: '50%', 
                width: '6px', 
                height: '6px', 
                marginTop: '8px', 
                marginRight: '12px',
                flexShrink: 0
              }}></span>
              <span style={{ color: '#374151' }}>
                <Text strong style={{ color: '#1f2937' }}>Multi-Stage Workflows:</Text> JVC → Plant → CQS → Tech approval chains
              </span>
            </li>
            <li style={{ marginBottom: 12, display: 'flex', alignItems: 'flex-start' }}>
              <span style={{ 
                background: '#52c41a', 
                borderRadius: '50%', 
                width: '6px', 
                height: '6px', 
                marginTop: '8px', 
                marginRight: '12px',
                flexShrink: 0
              }}></span>
              <span style={{ color: '#374151' }}>
                <Text strong style={{ color: '#1f2937' }}>Document Management:</Text> Secure storage with version control
              </span>
            </li>
            <li style={{ marginBottom: 12, display: 'flex', alignItems: 'flex-start' }}>
              <span style={{ 
                background: '#722ed1', 
                borderRadius: '50%', 
                width: '6px', 
                height: '6px', 
                marginTop: '8px', 
                marginRight: '12px',
                flexShrink: 0
              }}></span>
              <span style={{ color: '#374151' }}>
                <Text strong style={{ color: '#1f2937' }}>Query Resolution:</Text> Structured Q&A system with SLA tracking
              </span>
            </li>
            <li style={{ marginBottom: 12, display: 'flex', alignItems: 'flex-start' }}>
              <span style={{ 
                background: '#fa8c16', 
                borderRadius: '50%', 
                width: '6px', 
                height: '6px', 
                marginTop: '8px', 
                marginRight: '12px',
                flexShrink: 0
              }}></span>
              <span style={{ color: '#374151' }}>
                <Text strong style={{ color: '#1f2937' }}>Audit Compliance:</Text> Complete audit trail and reporting
              </span>
            </li>
            <li style={{ marginBottom: 0, display: 'flex', alignItems: 'flex-start' }}>
              <span style={{ 
                background: '#f59e0b', 
                borderRadius: '50%', 
                width: '6px', 
                height: '6px', 
                marginTop: '8px', 
                marginRight: '12px',
                flexShrink: 0
              }}></span>
              <span style={{ color: '#374151' }}>
                <Text strong style={{ color: '#1f2937' }}>Role-Based Security:</Text> Granular access control and permissions
              </span>
            </li>
          </ul>
        </Col>
      </Row>
    </Card>

  </div>
);

export default Home;
