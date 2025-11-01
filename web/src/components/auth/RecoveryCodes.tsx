/**
 * Recovery Codes Component
 *
 * Display and download recovery codes for E2E tier users.
 */

import React, { useState } from 'react';
import {
  DocumentArrowDownIcon,
  DocumentDuplicateIcon,
  CheckCircleIcon,
  ExclamationTriangleIcon
} from '@heroicons/react/24/outline';
import { Button } from '@/components/common/Button';
import { Alert } from '@/components/common/Alert';
import { Card } from '@/components/common/Card';

// ============================================================================
// Component
// ============================================================================

interface RecoveryCodesProps {
  codes: string[];
  onComplete: () => void;
}

export function RecoveryCodes({ codes, onComplete }: RecoveryCodesProps) {
  const [confirmed, setConfirmed] = useState(false);
  const [copied, setCopied] = useState(false);

  /**
   * Copy codes to clipboard
   */
  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(codes.join('\n'));
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy codes:', err);
    }
  };

  /**
   * Download codes as text file
   */
  const handleDownload = () => {
    const blob = new Blob([codes.join('\n')], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `diary-recovery-codes-${new Date().toISOString().split('T')[0]}.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  /**
   * Print codes
   */
  const handlePrint = () => {
    const printWindow = window.open('', '_blank');
    if (printWindow) {
      printWindow.document.write(`
        <!DOCTYPE html>
        <html>
          <head>
            <title>Recovery Codes</title>
            <style>
              body {
                font-family: system-ui, -apple-system, sans-serif;
                padding: 40px;
                max-width: 600px;
                margin: 0 auto;
              }
              h1 {
                margin-bottom: 20px;
              }
              .warning {
                background: #fef3c7;
                border-left: 4px solid #f59e0b;
                padding: 16px;
                margin-bottom: 20px;
              }
              .codes {
                display: grid;
                grid-template-columns: 1fr 1fr;
                gap: 8px;
                margin: 20px 0;
              }
              .code {
                font-family: 'Courier New', monospace;
                font-size: 14px;
                background: #f3f4f6;
                padding: 8px;
                border-radius: 4px;
              }
              .footer {
                margin-top: 40px;
                font-size: 12px;
                color: #6b7280;
              }
            </style>
          </head>
          <body>
            <h1>Personal Diary Recovery Codes</h1>
            <div class="warning">
              <strong>⚠ Important:</strong> Store these codes in a safe place. You'll need them to recover your account if you forget your password.
            </div>
            <div class="codes">
              ${codes.map((code) => `<div class="code">${code}</div>`).join('')}
            </div>
            <div class="footer">
              <p>Generated: ${new Date().toLocaleDateString()}</p>
              <p>Each code can only be used once. Keep them secure and private.</p>
            </div>
          </body>
        </html>
      `);
      printWindow.document.close();
      printWindow.print();
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="text-center">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-primary/10">
          <ExclamationTriangleIcon className="h-8 w-8 text-primary" />
        </div>
        <h2 className="text-2xl font-bold">Save Your Recovery Codes</h2>
        <p className="mt-2 text-muted-foreground">
          These codes are your only way to recover your account if you forget your password
        </p>
      </div>

      {/* Warning Alert */}
      <Alert variant="warning" title="Critical - Read Carefully">
        <ul className="mt-2 space-y-1 text-sm">
          <li>• Your diary is encrypted end-to-end with your password</li>
          <li>• If you forget your password, we cannot reset it for you</li>
          <li>• These recovery codes are the ONLY way to regain access</li>
          <li>• Each code can only be used once</li>
          <li>• Store them somewhere safe and secure</li>
        </ul>
      </Alert>

      {/* Recovery Codes */}
      <Card>
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-medium">Your Recovery Codes</h3>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={handleCopy}
                leftIcon={
                  copied ? (
                    <CheckCircleIcon className="h-4 w-4" />
                  ) : (
                    <DocumentDuplicateIcon className="h-4 w-4" />
                  )
                }
              >
                {copied ? 'Copied!' : 'Copy'}
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={handleDownload}
                leftIcon={<DocumentArrowDownIcon className="h-4 w-4" />}
              >
                Download
              </Button>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-2 rounded-md bg-muted p-4">
            {codes.map((code, index) => (
              <div
                key={index}
                className="rounded bg-background px-3 py-2 font-mono text-sm"
              >
                {code}
              </div>
            ))}
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={handlePrint}
            fullWidth
          >
            Print Codes
          </Button>
        </div>
      </Card>

      {/* Confirmation */}
      <div className="space-y-4">
        <label className="flex items-start gap-3 rounded-lg border p-4 cursor-pointer hover:bg-accent">
          <input
            type="checkbox"
            checked={confirmed}
            onChange={(e) => setConfirmed(e.target.checked)}
            className="mt-1 rounded border-input focus:ring-2 focus:ring-ring focus:ring-offset-2"
          />
          <div className="flex-1 text-sm">
            <p className="font-medium">I have saved my recovery codes</p>
            <p className="mt-1 text-muted-foreground">
              I understand that I won't be able to access my account without my password or these recovery codes.
            </p>
          </div>
        </label>

        <Button
          onClick={onComplete}
          fullWidth
          disabled={!confirmed}
        >
          Continue to Dashboard
        </Button>
      </div>
    </div>
  );
}
