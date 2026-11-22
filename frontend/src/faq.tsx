import { useMemo, useState } from 'react'
import { ChevronDown, HelpCircle, Search, X } from 'lucide-react'
import { PageHeading } from './components'

interface FaqItem {
  question: string
  answer: string
}

interface FaqSection {
  title: string
  questions: FaqItem[]
}

const faqSections: FaqSection[] = [
  {
    title: 'Getting started',
    questions: [
      {
        question: 'Who can use Smylio?',
        answer: 'Smylio is an owner-managed platform designed for property owners and tenants. When creating an account, you can select your starting role (Owner or Tenant), and you can activate the other role at any time.',
      },
      {
        question: 'How do I create an account?',
        answer: 'Choose Create an account on the sign-in page. Enter your full name, select Next, then provide your email, password, phone number, and starting account type.',
      },
      {
        question: 'How do I update my name, email, or phone number?',
        answer: 'Open your workspace menu and choose Profile & Account. You can review your details and update your name, email address, and phone number there.',
      },
      {
        question: 'How do I switch between dark and light appearance?',
        answer: 'Use the theme button (sun/moon icon) in the top bar to toggle themes. Smylio remembers your selection on this device.',
      },
    ],
  },
  {
    title: 'Workspaces & dual-role switching',
    questions: [
      {
        question: 'Can I be both an Owner and a Tenant on the same account?',
        answer: 'Yes! Smylio allows a single account to have both Owner Workspace and Tenant Portal capabilities. You do not need two separate accounts, different email addresses, or different passwords.',
      },
      {
        question: 'How do I activate the alternate workspace (Owner or Tenant)?',
        answer: 'Open the workspace dropdown at the top of the sidebar (or navigate to Profile & Account) and choose "Activate". You will be taken to a dedicated full-page activation view detailing all capabilities and data safety guarantees. Choose "Confirm & Activate" to enable it instantly.',
      },
      {
        question: 'Will activating another workspace affect or mix my existing data?',
        answer: 'Not at all. Your data is strictly isolated across independent database domains: owned properties, tenant leases, and rent approvals stay in your Owner Workspace, while your leased home, rent dues, and personal maintenance tickets stay in your Tenant Portal.',
      },
      {
        question: 'How do I switch between workspaces?',
        answer: 'Once you have enabled both roles, you can switch instantaneously at any time: 1) Open the workspace dropdown in the sidebar and click the desired workspace, 2) Click the quick switcher button in the top bar, or 3) Press the keyboard shortcut Alt + W.',
      },
      {
        question: 'What do the badges in the workspace dropdown mean?',
        answer: 'The badges alert you to pending items waiting in your inactive workspace so you never miss an important event. For example, if you are acting as a Tenant, the badge will alert you if an owner cash payment is pending approval or if a ticket is open on one of your properties.',
      },
      {
        question: 'What happens when I switch workspaces across multiple browser tabs?',
        answer: 'Smylio synchronizes your active workspace across all open browser tabs in real time using channel broadcast events. Switching roles in one tab immediately updates your other tabs without causing session or permission errors.',
      },
    ],
  },
  {
    title: 'For property owners',
    questions: [
      {
        question: 'How do I add a property?',
        answer: 'Open Properties and choose "Add property". Enter its title, address, type (Flat, Room, Building), monthly rent, description, and configure property amenities (e.g. Parking, Wi-Fi, Gym).',
      },
      {
        question: 'How do I connect a tenant to a property?',
        answer: 'Ask the tenant to share their Smylio Tenant ID (e.g., TNT-8K3P9M2W). In Tenants & leases, search for the tenant using that code, then create a lease with agreed rent, anniversary start date, grace days, and late fine rate.',
      },
      {
        question: 'Can I lease a property to myself?',
        answer: 'No. Smylio enforces strict self-leasing guards. An owner cannot search their own Tenant Code or create a lease where they are both the landlord and the tenant.',
      },
      {
        question: 'What information is stored on a lease?',
        answer: 'A lease connects a tenant to a property and records its start date, optional end date, agreed monthly rent, late-fee grace days, per-day fine, and active status.',
      },
      {
        question: 'How does rent billing work for owners?',
        answer: 'Smylio automatically bills rent in arrears based on each lease\'s monthly anniversary date (using IST midnight scheduling). You can also click "Generate rent" on the overview or rent page to immediately run an arrears catch-up check.',
      },
      {
        question: 'How do I review a tenant’s cash payment claim?',
        answer: 'Cash payment claims appear under Cash payment approvals on your overview and in the Rent section. Choose Accept to stamp the bill paid (freezing any fines) or Reject to decline it (which restores the late status if overdue).',
      },
      {
        question: 'How can I track rent that is overdue?',
        answer: 'The Rent page shows status badges, due dates, amounts, and accrued late fines. The owner overview also summarizes total outstanding rent and fines across all properties.',
      },
      {
        question: 'How do I manage maintenance requests?',
        answer: 'Open Maintenance to review tenant tickets, update their status (Open → In progress → Resolved), and communicate directly with the tenant via the ticket discussion thread.',
      },
      {
        question: 'How do I post an announcement or notice?',
        answer: 'Open Notices and click "Post a notice". Enter a title and message. You can broadcast it to all your leased tenants or target a specific property.',
      },
      {
        question: 'Can I edit or delete a property?',
        answer: 'You can edit property details and amenities at any time. However, properties with active leases, historical payments, tickets, or notices are protected from deletion to preserve financial and operational audit records.',
      },
    ],
  },
  {
    title: 'For tenants',
    questions: [
      {
        question: 'Where can I find my Tenant ID?',
        answer: 'Your unique Tenant ID (e.g., TNT-8K3P9M2W) is prominently displayed on the My home page with one-click copy and instant sharing buttons for WhatsApp and Telegram.',
      },
      {
        question: 'Where can I view my lease and property details?',
        answer: 'Open My home to see your leased flat, landlord contact details, assigned amenities, and lease term summaries.',
      },
      {
        question: 'How can I see what rent I owe?',
        answer: 'Open Rent to see your total balance due, earliest upcoming due date, unpaid breakdown with any late fines, and complete receipt history.',
      },
      {
        question: 'How do I pay rent online?',
        answer: 'On the Rent page, choose "Pay" for an eligible rent item, select Online payment, and confirm to simulate an instant payment with a unique transaction reference ID.',
      },
      {
        question: 'How do I report a cash rent payment?',
        answer: 'Choose "Pay" and select "Cash claim". Smylio registers your payment claim and temporarily freezes late fines while awaiting your landlord\'s verification.',
      },
      {
        question: 'Why was a late fine added to my rent?',
        answer: 'Each lease specifies grace days and a daily fine rate. If rent is not paid after the grace period expires, the daily fine accrues automatically on the balance.',
      },
      {
        question: 'How do I report a repair or maintenance issue?',
        answer: 'Open Maintenance and submit a ticket with a title, description, and priority (Low, Medium, High). You can track the status and communicate with your landlord.',
      },
      {
        question: 'How does comment attribution work in maintenance discussions?',
        answer: 'In ticket discussion threads, author titles are determined contextually by property ownership: if you own the property, you appear as "Owner"; if you rent the home, you appear as "Tenant", ensuring full accuracy even if you have multiple roles on your account.',
      },
      {
        question: 'Where can I read announcements from my landlord?',
        answer: 'Open Notices to view all broadcast and property-targeted announcements posted by your landlord. Recent notices also appear on your My home dashboard.',
      },
    ],
  },
  {
    title: 'Notifications & system indicators',
    questions: [
      {
        question: 'What does the colored vertical line on notification messages indicate?',
        answer: 'Notifications feature a 4px accent border line for rapid status recognition: Green indicates success (e.g., payment recorded or status updated), Red indicates errors or validation issues, Amber indicates warnings or pending confirmations, and Cyan/accent indicates informational notices.',
      },
      {
        question: 'Can I dismiss notifications early?',
        answer: 'Yes. Click the ✕ button on any notification message to dismiss it immediately. If left alone, auto-dismissing notifications feature an animated countdown timer line at the bottom showing how much time remains.',
      },
      {
        question: 'How do I know when I have switched workspaces?',
        answer: 'Whenever you toggle workspaces (via the sidebar, top bar, or Alt + W), a green confirmation banner appears at the top of your screen confirming "Switched to Owner Workspace." or "Switched to Tenant Portal."',
      },
    ],
  },
  {
    title: 'Payments, security & account questions',
    questions: [
      {
        question: 'Why is my lease or property not showing up?',
        answer: 'A property appears in your Tenant Portal after your landlord creates an active lease linked to your Tenant ID. Check that your landlord has your exact Tenant ID (including the TNT- prefix) and has activated your lease.',
      },
      {
        question: 'Why is my cash payment still awaiting approval?',
        answer: 'Cash payment claims require landlord confirmation. Your landlord will see the claim in their Cash payment approvals box and can accept or reject it.',
      },
      {
        question: 'How does Smylio protect my credentials and session?',
        answer: 'Smylio uses BCrypt (cost factor 12) password hashing with timing-attack mitigation. Authentication uses short-lived 15-minute access JWTs kept in browser memory (never localStorage), paired with rotating, single-use refresh cookies. Changing your password immediately revokes all active sessions across all devices.',
      },
      {
        question: 'What should I do if a page displays an error?',
        answer: 'Try refreshing the page or checking your network connection. If the issue persists, verify that you are in the correct workspace for the action you are trying to perform.',
      },
      {
        question: 'How do I sign out?',
        answer: 'Choose the sign-out button in the top bar or sidebar and confirm. Your session tokens are revoked and you can sign in again anytime.',
      },
    ],
  },
]

export function FaqPage() {
  const [openQuestions, setOpenQuestions] = useState<Set<string>>(() => new Set())
  const [searchQuery, setSearchQuery] = useState('')

  const trimmedQuery = searchQuery.trim().toLowerCase()

  const filteredSections = useMemo(() => {
    if (!trimmedQuery) {
      return faqSections
    }

    return faqSections
      .map((section) => {
        const matchingQuestions = section.questions.filter(
          (q) =>
            q.question.toLowerCase().includes(trimmedQuery) ||
            q.answer.toLowerCase().includes(trimmedQuery) ||
            section.title.toLowerCase().includes(trimmedQuery)
        )
        return {
          ...section,
          questions: matchingQuestions,
        }
      })
      .filter((section) => section.questions.length > 0)
  }, [trimmedQuery])

  const totalResults = useMemo(() => {
    return filteredSections.reduce((sum, section) => sum + section.questions.length, 0)
  }, [filteredSections])

  const allVisibleIds = useMemo(() => {
    const ids: string[] = []
    filteredSections.forEach((section, sIdx) => {
      section.questions.forEach((_, qIdx) => {
        ids.push(`faq-${sIdx}-${qIdx}`)
      })
    })
    return ids
  }, [filteredSections])

  function toggleQuestion(id: string) {
    setOpenQuestions((prev) => {
      const next = new Set(prev)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return next
    })
  }

  function handleToggleAll() {
    if (openQuestions.size > 0) {
      setOpenQuestions(new Set())
    } else {
      setOpenQuestions(new Set(allVisibleIds))
    }
  }

  return (
    <section className="page-section faq-page">
      <PageHeading
        eyebrow="HELP CENTER"
        title="Frequently asked questions"
        subtitle="Quick answers about managing properties, leases, rent, notifications, and dual workspaces."
      />

      {/* FAQ Search Bar */}
      <div className="faq-search-wrap">
        <Search size={18} className="faq-search-icon" aria-hidden="true" />
        <input
          type="search"
          className="faq-search-input"
          placeholder="Search questions or keywords (e.g. switch workspace, tenant ID, cash claim, notice)..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          aria-label="Search frequently asked questions"
        />
        {searchQuery && (
          <button
            type="button"
            className="faq-search-clear"
            onClick={() => setSearchQuery('')}
            aria-label="Clear search query"
            title="Clear search"
          >
            <X size={15} />
          </button>
        )}
      </div>

      {/* Results summary and Toggle All */}
      {trimmedQuery && (
        <div className="faq-results-summary">
          <span>
            Found <strong>{totalResults}</strong> result{totalResults === 1 ? '' : 's'} for &ldquo;{searchQuery}&rdquo;
          </span>
          {totalResults > 0 && (
            <button
              type="button"
              className="faq-toggle-all-button"
              onClick={handleToggleAll}
            >
              {openQuestions.size > 0 ? 'Collapse all' : 'Expand all'}
            </button>
          )}
        </div>
      )}

      {/* Empty State */}
      {filteredSections.length === 0 ? (
        <div className="empty-state surface" style={{ padding: '42px 24px', textAlign: 'center' }}>
          <HelpCircle size={40} style={{ margin: '0 auto 14px', color: 'var(--muted)' }} />
          <strong>No matching questions found</strong>
          <p style={{ marginTop: '6px' }}>
            We couldn&rsquo;t find any FAQs matching &ldquo;{searchQuery}&rdquo;. Try searching for general terms like
            &ldquo;rent&rdquo;, &ldquo;workspace&rdquo;, &ldquo;code&rdquo;, or &ldquo;lease&rdquo;.
          </p>
          <button
            type="button"
            className="button button-primary"
            onClick={() => setSearchQuery('')}
            style={{ marginTop: '16px' }}
          >
            Clear search
          </button>
        </div>
      ) : (
        <div className="faq-sections">
          {filteredSections.map((section, sectionIndex) => (
            <section className="faq-section surface" key={section.title}>
              <h2>{section.title}</h2>
              <div className="faq-list">
                {section.questions.map(({ question, answer }, questionIndex) => {
                  const id = `faq-${sectionIndex}-${questionIndex}`
                  const isOpen = openQuestions.has(id) || (Boolean(trimmedQuery) && totalResults <= 5)
                  return (
                    <article className={`faq-item ${isOpen ? 'faq-item-open' : ''}`} key={question}>
                      <h3>
                        <button
                          type="button"
                          className="faq-question"
                          aria-expanded={isOpen}
                          aria-controls={`${id}-answer`}
                          onClick={() => toggleQuestion(id)}
                        >
                          <span>{question}</span>
                          <ChevronDown size={18} aria-hidden="true" />
                        </button>
                      </h3>
                      {isOpen && (
                        <div className="faq-answer" id={`${id}-answer`}>
                          <p>{answer}</p>
                        </div>
                      )}
                    </article>
                  )
                })}
              </div>
            </section>
          ))}
        </div>
      )}
    </section>
  )
}
