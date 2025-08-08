import { Skeleton, Card, Row, Col, Space } from 'antd';
import React from 'react';

const PageSkeleton = () => {
  return (
    <div style={{ padding: '24px' }}>
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        {/* Page Header Skeleton */}
        <div>
          <Skeleton.Input 
            style={{ width: 200, height: 32 }} 
            active 
            size="large" 
          />
          <div style={{ marginTop: 8 }}>
            <Skeleton.Input 
              style={{ width: 400, height: 20 }} 
              active 
              size="small" 
            />
          </div>
        </div>

        {/* Action Bar Skeleton */}
        <Row gutter={16}>
          <Col span={6}>
            <Skeleton.Input style={{ width: '100%' }} active />
          </Col>
          <Col span={6}>
            <Skeleton.Input style={{ width: '100%' }} active />
          </Col>
          <Col span={6}>
            <Skeleton.Input style={{ width: '100%' }} active />
          </Col>
          <Col span={6}>
            <Skeleton.Input style={{ width: '100%' }} active />
          </Col>
        </Row>

        {/* Main Content Skeleton */}
        <Card>
          <Skeleton 
            active 
            paragraph={{ rows: 8 }} 
            title={{ width: '60%' }}
          />
        </Card>

        {/* Additional Content Cards */}
        <Row gutter={16}>
          <Col span={12}>
            <Card>
              <Skeleton 
                active 
                paragraph={{ rows: 4 }} 
                title={{ width: '40%' }}
              />
            </Card>
          </Col>
          <Col span={12}>
            <Card>
              <Skeleton 
                active 
                paragraph={{ rows: 4 }} 
                title={{ width: '40%' }}
              />
            </Card>
          </Col>
        </Row>
      </Space>
    </div>
  );
};

export default PageSkeleton;