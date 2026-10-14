import React from 'react';
import Header from '../common/Header';
import Footer from '../common/Footer';
import ErrorAlert from '../common/ErrorAlert';

export default function AppLayout({ children }) {
  return (
    <div className="app-wrapper">
      <Header />
      <main className="main-content" id="main-content-region">
        <div className="container px-3">
          {/* Global error presentation banner */}
          <ErrorAlert />
          {children}
        </div>
      </main>
      <Footer />
    </div>
  );
}
