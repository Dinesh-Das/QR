import { notification } from 'antd';

export const notifySuccess = (message, description) => {
  notification.success({ message, description });
};

export const notifyError = (message, description) => {
  notification.error({ message, description });
};

export const notifyInfo = (message, description) => {
  notification.info({ message, description });
};
