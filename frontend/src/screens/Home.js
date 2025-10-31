import {
  DashboardOutlined,
  FileTextOutlined,
  TeamOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  RocketOutlined,
  SafetyOutlined,
  AuditOutlined,
  GlobalOutlined
} from '@ant-design/icons';
import {
  Card,
  Row,
  Col,
  Typography,
  Button,
  Badge,
  Timeline,
  Progress,
  Divider
} from 'antd';
import React from 'react';
import { Link } from 'react-router-dom';

const { Title, Paragraph, Text } = Typography;

const Home = () => (
  <div style={{ padding: 24, background: '#f5f5f5', minHeight: '100vh' }}>
    {/* Hero Section */}
    <div
      style={{
        background: 'linear-gradient(135deg, #1890ff 0%, #096dd9 100%)',
        borderRadius: 12,
        padding: '48px 32px',
        marginBottom: 32,
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
      <Col xs={24} lg={16}>
        <Card
          title={
            <>
              <GlobalOutlined /> Key Features
            </>
          }
          style={{ height: '100%' }}
        >
          <Row gutter={[16, 16]}>
            <Col xs={24} md={12}>
              <Card size="small" hoverable>
                <DashboardOutlined style={{ fontSize: 24, color: '#1890ff', marginBottom: 8 }} />
                <Title level={5}>Workflow Management</Title>
                <Paragraph>
                  Track and manage quality risk workflows from initiation to completion across JVC,
                  Plant, CQS, and Tech teams.
                </Paragraph>
                <Button type="primary" size="small" as={Link} to="/qrmfg/dashboard">
                  View Dashboard
                </Button>
              </Card>
            </Col>
            <Col xs={24} md={12}>
              <Card size="small" hoverable>
                <FileTextOutlined style={{ fontSize: 24, color: '#52c41a', marginBottom: 8 }} />
                <Title level={5}>Document Control</Title>
                <Paragraph>
                  Centralized document management with version control, access tracking, and secure
                  storage.
                </Paragraph>
                <Button type="primary" size="small" as={Link} to="/qrmfg/documents">
                  Manage Documents
                </Button>
              </Card>
            </Col>
            <Col xs={24} md={12}>
              <Card size="small" hoverable>
                <AuditOutlined style={{ fontSize: 24, color: '#722ed1', marginBottom: 8 }} />
                <Title level={5}>Query Management</Title>
                <Paragraph>
                  Streamlined query resolution system with team assignments and SLA tracking.
                </Paragraph>
                <Button type="primary" size="small" as={Link} to="/qrmfg/queries">
                  View Queries
                </Button>
              </Card>
            </Col>
            <Col xs={24} md={12}>
              <Card size="small" hoverable>
                <TeamOutlined style={{ fontSize: 24, color: '#fa8c16', marginBottom: 8 }} />
                <Title level={5}>Role-Based Access</Title>
                <Paragraph>
                  Comprehensive RBAC system ensuring secure access control and audit compliance.
                </Paragraph>
                <Button type="primary" size="small" as={Link} to="/qrmfg/admin">
                  Admin Panel
                </Button>
              </Card>
            </Col>
          </Row>
        </Card>
      </Col>

      <Col xs={24} lg={8}>
        <Card
          title={
            <>
              <ClockCircleOutlined /> Recent Activity
            </>
          }
          style={{ height: '100%' }}
        >
          <Timeline
            items={[
              {
                color: 'green',
                children: (
                  <div>
                    <Text strong>System Initialized</Text>
                    <br />
                    <Text type="secondary">QRMFG application started successfully</Text>
                  </div>
                )
              },
              {
                color: 'blue',
                children: (
                  <div>
                    <Text strong>Database Connected</Text>
                    <br />
                    <Text type="secondary">Oracle database connection established</Text>
                  </div>
                )
              },
              {
                color: 'gray',
                children: (
                  <div>
                    <Text strong>Ready for Operations</Text>
                    <br />
                    <Text type="secondary">All systems operational</Text>
                  </div>
                )
              }
            ]}
          />
        </Card>
      </Col>
    </Row>


    {/* Project Information */}
    <Row gutter={[24, 24]}>
      <Col xs={24} lg={12}>
        <Card
          title={
            <>
              <SafetyOutlined /> About QRMFG
            </>
          }
        >
          <Paragraph>
            <Text strong>QRMFG (Quality Risk Management for Manufacturing)</Text> is an
            enterprise-grade workflow management system designed specifically for manufacturing
            quality assurance processes.
          </Paragraph>

          <Divider />

          <Title level={5}>Key Capabilities:</Title>
          <ul style={{ paddingLeft: 20 }}>
            <li>
              <Text strong>Multi-Stage Workflows:</Text> JVC → Plant → CQS → Tech approval chains
            </li>
            <li>
              <Text strong>Document Management:</Text> Secure storage with version control
            </li>
            <li>
              <Text strong>Query Resolution:</Text> Structured Q&A system with SLA tracking
            </li>
            <li>
              <Text strong>Audit Compliance:</Text> Complete audit trail and reporting
            </li>
            <li>
              <Text strong>Role-Based Security:</Text> Granular access control and permissions
            </li>
          </ul>
        </Card>
      </Col>

      <Col xs={24} lg={12}>
        <Card
          title={
            <>
              <CheckCircleOutlined /> System Status
            </>
          }
        >
          <div style={{ marginBottom: 16 }}>
            <Text>Database Connection</Text>
            <Progress percent={100} status="success" showInfo={false} />
          </div>

          <div style={{ marginBottom: 16 }}>
            <Text>Application Health</Text>
            <Progress percent={100} status="success" showInfo={false} />
          </div>

          <div style={{ marginBottom: 16 }}>
            <Text>Security Status</Text>
            <Progress percent={100} status="success" showInfo={false} />
          </div>

          <Divider />

          <Row gutter={16}>
            <Col span={12}>
              <Badge status="success" text="Services Online" />
            </Col>
            <Col span={12}>
              <Badge status="success" text="Database Ready" />
            </Col>
            <Col span={12}>
              <Badge status="success" text="Security Active" />
            </Col>
            <Col span={12}>
              <Badge status="success" text="Monitoring On" />
            </Col>
          </Row>
        </Card>
      </Col>
    </Row>

  </div>
);

export default Home;
