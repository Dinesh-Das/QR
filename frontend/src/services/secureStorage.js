import CryptoJS from 'crypto-js';

/**
 * SecureTokenStorage class provides encrypted token storage using AES encryption
 * and sessionStorage for better security compared to plain localStorage.
 */
class SecureTokenStorage {
  static TOKEN_KEY = 'qrmfg_secure_token';
  static REFRESH_TOKEN_KEY = 'qrmfg_secure_refresh_token';
  static SECRET_KEY = process.env.REACT_APP_ENCRYPTION_KEY || 'qrmfg-default-encryption-key-2024';

  /**
   * Encrypts and stores a token in sessionStorage
   * @param {string} token - The JWT token to store
   * @throws {Error} If token storage fails
   */
  static setToken(token) {
    if (!token || typeof token !== 'string') {
      throw new Error('Token must be a non-empty string');
    }

    try {
      const encrypted = CryptoJS.AES.encrypt(token, this.SECRET_KEY).toString();
      sessionStorage.setItem(this.TOKEN_KEY, encrypted);
    } catch (error) {
      console.error('Failed to store token securely:', error.message);
      throw new Error('Token storage failed');
    }
  }

  /**
   * Retrieves and decrypts a token from sessionStorage
   * @returns {string|null} The decrypted token or null if not found/invalid
   */
  static getToken() {
    try {
      const encrypted = sessionStorage.getItem(this.TOKEN_KEY);
      if (!encrypted) {
        return null;
      }

      const decrypted = CryptoJS.AES.decrypt(encrypted, this.SECRET_KEY);
      const token = decrypted.toString(CryptoJS.enc.Utf8);

      if (!token) {
        // If decryption results in empty string, remove the invalid token
        this.removeToken();
        return null;
      }

      return token;
    } catch (error) {
      console.error('Failed to retrieve token:', error.message);
      this.removeToken(); // Clean up invalid token
      return null;
    }
  }

  /**
   * Removes the token from sessionStorage
   */
  static removeToken() {
    try {
      sessionStorage.removeItem(this.TOKEN_KEY);
    } catch (error) {
      console.error('Failed to remove token:', error.message);
    }
  }

  /**
   * Checks if a token exists in sessionStorage
   * @returns {boolean} True if token exists, false otherwise
   */
  static hasToken() {
    try {
      return sessionStorage.getItem(this.TOKEN_KEY) !== null;
    } catch (error) {
      console.error('Failed to check token existence:', error.message);
      return false;
    }
  }

  /**
   * Encrypts and stores a refresh token in sessionStorage
   * @param {string} refreshToken - The refresh token to store
   * @throws {Error} If refresh token storage fails
   */
  static setRefreshToken(refreshToken) {
    if (!refreshToken || typeof refreshToken !== 'string') {
      throw new Error('Refresh token must be a non-empty string');
    }

    try {
      const encrypted = CryptoJS.AES.encrypt(refreshToken, this.SECRET_KEY).toString();
      sessionStorage.setItem(this.REFRESH_TOKEN_KEY, encrypted);
    } catch (error) {
      console.error('Failed to store refresh token securely:', error.message);
      throw new Error('Refresh token storage failed');
    }
  }

  /**
   * Retrieves and decrypts a refresh token from sessionStorage
   * @returns {string|null} The decrypted refresh token or null if not found/invalid
   */
  static getRefreshToken() {
    try {
      const encrypted = sessionStorage.getItem(this.REFRESH_TOKEN_KEY);
      if (!encrypted) {
        return null;
      }

      const decrypted = CryptoJS.AES.decrypt(encrypted, this.SECRET_KEY);
      const refreshToken = decrypted.toString(CryptoJS.enc.Utf8);

      if (!refreshToken) {
        // If decryption results in empty string, remove the invalid token
        this.removeRefreshToken();
        return null;
      }

      return refreshToken;
    } catch (error) {
      console.error('Failed to retrieve refresh token:', error.message);
      this.removeRefreshToken(); // Clean up invalid token
      return null;
    }
  }

  /**
   * Removes the refresh token from sessionStorage
   */
  static removeRefreshToken() {
    try {
      sessionStorage.removeItem(this.REFRESH_TOKEN_KEY);
    } catch (error) {
      console.error('Failed to remove refresh token:', error.message);
    }
  }

  /**
   * Checks if a refresh token exists in sessionStorage
   * @returns {boolean} True if refresh token exists, false otherwise
   */
  static hasRefreshToken() {
    try {
      return sessionStorage.getItem(this.REFRESH_TOKEN_KEY) !== null;
    } catch (error) {
      console.error('Failed to check refresh token existence:', error.message);
      return false;
    }
  }

  /**
   * Clears all stored tokens
   */
  static clearAll() {
    this.removeToken();
    this.removeRefreshToken();
  }
}

export default SecureTokenStorage;
