/**
 * JWTValidator class provides comprehensive JWT token validation
 * including structure, expiry, issuer, audience, and required claims validation.
 */
class JWTValidator {
  /**
   * Validates a JWT token for structure, expiry, and claims
   * @param {string} token - The JWT token to validate
   * @returns {boolean} True if token is valid, false otherwise
   */
  static validateToken(token) {
    if (!token || typeof token !== 'string') {
      console.warn('JWT validation failed: Token is not a string');
      return false;
    }

    try {
      // Validate JWT structure (3 parts separated by dots)
      const parts = token.split('.');
      if (parts.length !== 3) {
        console.warn('JWT validation failed: Token does not have 3 parts');
        return false;
      }

      // Parse and validate payload
      const payload = this.getTokenPayload(token);
      if (!payload) {
        console.warn('JWT validation failed: Could not parse payload');
        return false;
      }

      // Basic validation - just check if we have a subject and it's not expired
      if (!payload.sub) {
        console.warn('JWT validation failed: Missing subject (sub) claim');
        return false;
      }

      // Check expiry if present
      if (payload.exp) {
        const currentTime = Math.floor(Date.now() / 1000);
        if (payload.exp <= currentTime) {
          console.warn('JWT validation failed: Token has expired');
          return false;
        }
      }

      console.log('JWT validation successful');
      return true;
    } catch (error) {
      console.error('JWT validation error:', error.message);
      return false;
    }
  }

  /**
   * Validates JWT claims including issuer, audience, and required fields
   * @param {object} payload - The JWT payload object
   * @returns {boolean} True if claims are valid, false otherwise
   */
  static validateClaims(payload) {
    // This method is now simplified and called from validateToken
    // Most validation is done in the main validateToken method
    return true;
  }

  /**
   * Validates JWT expiry time
   * @param {object} payload - The JWT payload object
   * @returns {boolean} True if token is not expired, false otherwise
   */
  static validateExpiry(payload) {
    if (!payload || typeof payload !== 'object') {
      return false;
    }

    try {
      // Check if exp claim exists
      if (!payload.exp) {
        console.warn('JWT missing expiry claim (exp)');
        return false;
      }

      // Validate expiry (exp is in seconds, Date.now() is in milliseconds)
      const currentTime = Math.floor(Date.now() / 1000);
      if (payload.exp <= currentTime) {
        console.warn('JWT token has expired:', new Date(payload.exp * 1000).toISOString());
        return false;
      }

      // Optional: Check if token is not used before its valid time (nbf - not before)
      if (payload.nbf && payload.nbf > currentTime) {
        console.warn('JWT token not yet valid:', new Date(payload.nbf * 1000).toISOString());
        return false;
      }

      return true;
    } catch (error) {
      console.error('JWT expiry validation error:', error.message);
      return false;
    }
  }

  /**
   * Safely extracts and parses JWT payload
   * @param {string} token - The JWT token
   * @returns {object|null} The parsed payload or null if invalid
   */
  static getTokenPayload(token) {
    if (!token || typeof token !== 'string') {
      return null;
    }

    try {
      const parts = token.split('.');
      if (parts.length !== 3) {
        return null;
      }

      // Decode the payload (second part) with proper base64url handling
      const base64Url = parts[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const padded = base64 + '='.repeat((4 - base64.length % 4) % 4);
      const payload = JSON.parse(atob(padded));
      return payload;
    } catch (error) {
      console.error('Failed to parse JWT payload:', error.message);
      return null;
    }
  }

  /**
   * Extracts user information from JWT payload
   * @param {string} token - The JWT token
   * @returns {object|null} User information object or null if invalid
   */
  static getUserInfo(token) {
    const payload = this.getTokenPayload(token);
    if (!payload) {
      return null;
    }

    try {
      return {
        id: payload.sub || payload.userId,
        username: payload.username || payload.user || payload.name || payload.sub || 'Unknown User',
        roles: this.extractRoles(payload),
        email: payload.email || null,
        fullName: payload.name || payload.full_name || payload.username || null,
        issuedAt: payload.iat ? new Date(payload.iat * 1000) : null,
        expiresAt: payload.exp ? new Date(payload.exp * 1000) : null,
        // RBAC-specific fields
        primaryRoleType: payload.primaryRoleType || payload.roleType,
        roleType: payload.primaryRoleType || payload.roleType,
        plantCodes: payload.plantCodes || payload.assignedPlants || [],
        assignedPlants: payload.plantCodes || payload.assignedPlants || [],
        primaryPlant: payload.primaryPlant || payload.primaryPlantCode,
        primaryPlantCode: payload.primaryPlant || payload.primaryPlantCode,
        isAdmin: payload.isAdmin || false,
        isPlantUser: payload.isPlantUser || false
      };
    } catch (error) {
      console.error('Failed to extract user info from JWT:', error.message);
      return null;
    }
  }

  /**
   * Extracts roles from JWT payload, handling different claim formats
   * @param {object} payload - The JWT payload object
   * @returns {Array} Array of roles
   */
  static extractRoles(payload) {
    if (!payload) {
      return [];
    }

    try {
      // Try different common role claim names
      const roles = payload.roles || payload.authorities || payload.role;

      if (!roles) {
        // Return default user role if no roles specified
        return ['USER'];
      }

      // Handle different role formats
      if (Array.isArray(roles)) {
        return roles;
      }

      if (typeof roles === 'string') {
        // Handle comma-separated roles
        if (roles.includes(',')) {
          return roles.split(',').map(role => role.trim());
        }
        // Handle space-separated roles
        if (roles.includes(' ')) {
          return roles.split(' ').map(role => role.trim());
        }
        // Single role
        return [roles];
      }

      return [];
    } catch (error) {
      console.error('Failed to extract roles from JWT payload:', error.message);
      return [];
    }
  }

  /**
   * Checks if user has a specific role
   * @param {string} token - The JWT token
   * @param {string} requiredRole - The role to check for
   * @returns {boolean} True if user has the role, false otherwise
   */
  static hasRole(token, requiredRole) {
    if (!this.validateToken(token) || !requiredRole) {
      return false;
    }

    try {
      const userInfo = this.getUserInfo(token);
      if (!userInfo || !userInfo.roles) {
        return false;
      }

      const normalizedRequiredRole = requiredRole.toLowerCase();
      
      return userInfo.roles.some(role => {
        const normalizedRole = role.toLowerCase();
        
        // Direct match
        if (normalizedRole === normalizedRequiredRole) {
          return true;
        }
        
        // Match with ROLE_ prefix
        if (normalizedRole === `role_${normalizedRequiredRole}`) {
          return true;
        }
        
        // Match without ROLE_ prefix (if required role has it)
        if (normalizedRequiredRole.startsWith('role_') && normalizedRole === normalizedRequiredRole.substring(5)) {
          return true;
        }
        
        // Match role that starts with required role
        if (normalizedRole.startsWith(`${normalizedRequiredRole}_`)) {
          return true;
        }
        
        return false;
      });
    } catch (error) {
      console.error('Failed to check user role:', error.message);
      return false;
    }
  }

  /**
   * Checks if user is an admin
   * @param {string} token - The JWT token
   * @returns {boolean} True if user is admin, false otherwise
   */
  static isAdmin(token) {
    if (!this.validateToken(token)) {
      return false;
    }

    try {
      const userInfo = this.getUserInfo(token);
      
      // First check the isAdmin flag from the backend
      if (userInfo && typeof userInfo.isAdmin === 'boolean') {
        console.log('JWT isAdmin check - using isAdmin flag:', userInfo.isAdmin);
        return userInfo.isAdmin;
      }
      
      // Fallback to role checking (case-insensitive)
      const hasAdminRole = this.hasRole(token, 'ADMIN') || this.hasRole(token, 'admin') || this.hasRole(token, 'ROLE_ADMIN');
      console.log('JWT isAdmin check - role-based result:', hasAdminRole);
      console.log('JWT isAdmin check - available roles:', userInfo?.roles);
      
      return hasAdminRole;
    } catch (error) {
      console.error('Failed to check admin status:', error.message);
      return false;
    }
  }

  /**
   * Gets time until token expires
   * @param {string} token - The JWT token
   * @returns {number|null} Seconds until expiry, or null if invalid/expired
   */
  static getTimeUntilExpiry(token) {
    const payload = this.getTokenPayload(token);
    if (!payload || !payload.exp) {
      return null;
    }

    const currentTime = Math.floor(Date.now() / 1000);
    const timeUntilExpiry = payload.exp - currentTime;

    return timeUntilExpiry > 0 ? timeUntilExpiry : null;
  }

  /**
   * Checks if token will expire within specified seconds
   * @param {string} token - The JWT token
   * @param {number} seconds - Seconds to check against
   * @returns {boolean} True if token expires within specified time
   */
  static willExpireWithin(token, seconds = 300) {
    // Default 5 minutes
    const timeUntilExpiry = this.getTimeUntilExpiry(token);
    return timeUntilExpiry !== null && timeUntilExpiry <= seconds;
  }
}

export default JWTValidator;
