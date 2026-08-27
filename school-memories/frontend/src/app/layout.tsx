import type { Metadata } from 'next';
import './globals.css';
import { Providers } from './providers';
import Nav from '@/components/Nav';
import Footer from '@/components/Footer';
import ServiceStatusBanner from '@/components/ServiceStatusBanner';

export const metadata: Metadata = {
  title: {
    default: 'Keepsake — the school memory community',
    template: '%s · Keepsake',
  },
  description:
    'Thirty questions, one school, a permanent archive of the people we were. Answer a question a day and keep your school memories forever.',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className="flex min-h-screen flex-col">
        {/* Apply the saved theme before React hydrates to avoid a flash of the wrong mode. */}
        <script
          dangerouslySetInnerHTML={{
            __html: `(function(){try{var t=localStorage.getItem('keepsake.theme');if(t==='dark'||(!t&&window.matchMedia('(prefers-color-scheme: dark)').matches))document.documentElement.classList.add('dark')}catch(e){}})()`,
          }}
        />
        <Providers>
          <Nav />
          <ServiceStatusBanner />
          <main className="flex-1">{children}</main>
          <Footer />
        </Providers>
      </body>
    </html>
  );
}
