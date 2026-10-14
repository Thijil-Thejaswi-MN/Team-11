import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import AppLayout from '../components/layout/AppLayout';

describe('AppLayout Component', () => {
  it('renders brand logo, header navigation, children content, and footer', () => {
    render(
      <BrowserRouter>
        <AuthProvider>
          <AppLayout>
            <div data-testid="test-child-content">Child Dashboard Content</div>
          </AppLayout>
        </AuthProvider>
      </BrowserRouter>
    );

    // Verify brand text in Header
    expect(screen.getByText(/InsureFlow/i)).toBeInTheDocument();

    // Verify child content
    expect(screen.getByTestId('test-child-content')).toHaveTextContent('Child Dashboard Content');

    // Verify footer text
    expect(screen.getByText(/Insurance Workflow Automation System/i)).toBeInTheDocument();
    expect(screen.getAllByText(/Team 11/i).length).toBeGreaterThanOrEqual(1);
  });
});
