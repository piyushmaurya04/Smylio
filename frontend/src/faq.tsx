import { useState } from 'react'
import { ChevronDown } from 'lucide-react'
import { PageHeading } from './components'

const faqSections = [
  {
    title: 'Getting started',
    questions: [
      {
        question: 'Who can use Smylio?',
        answer: 'Smylio has separate experiences for property owners and tenants. Choose the account type that matches your role when you create your account.',
      },
      {
        question: 'How do I create an account?',
        answer: 'Choose Create an account on the sign-in page. Enter your full name, select Next, then provide your email, password, phone number, and account type.',
      },
      {
        question: 'How do I update my name, email, or phone number?',
        answer: 'Open your workspace menu and choose Profile. You can review your details and update your name, email address, and phone number there.',
      },
      {
        question: 'How do I switch between dark and light appearance?',
        answer: 'Use the theme button in the top bar to switch themes. Smylio remembers your selection on this device.',
      },
    ],
  },
  {
    title: 'For property owners',
    questions: [
      {
        question: 'How do I add a property?',
        answer: 'Open Properties and choose the option to add a property. Enter its name, address, type, monthly rent, and any description or amenities you want to include.',
      },
      {
        question: 'How do I connect a tenant to a property?',
        answer: 'Ask the tenant to share their Smylio tenant ID. In Tenants & leases, find the tenant using that ID, then create a lease for the property.',
      },
      {
        question: 'What information is stored on a lease?',
        answer: 'A lease connects a tenant to a property and records its start and end dates, monthly rent, late-fee grace days, daily fine, and status.',
      },
      {
        question: 'How do I generate rent entries?',
        answer: 'Use Generate rent on the owner overview or rent page. Smylio creates the rent entries based on active leases and reports when there is nothing new to generate.',
      },
      {
        question: 'How do I review a tenant’s cash payment claim?',
        answer: 'Cash claims awaiting review appear under Cash payment approvals on your overview and in Rent. Choose Accept to mark a claim paid or Reject to decline it.',
      },
      {
        question: 'How can I track rent that is overdue?',
        answer: 'The Rent page shows rent status, due dates, amounts, and any applicable fine. The owner overview also summarizes outstanding rent.',
      },
      {
        question: 'How do I respond to a maintenance request?',
        answer: 'Open Maintenance to review requests, change their status, and communicate with the tenant in the request comments.',
      },
      {
        question: 'How do I send an announcement to tenants?',
        answer: 'Open Notices and create a notice with a title and message. You can optionally associate it with a property.',
      },
      {
        question: 'Can I edit or remove a property or notice?',
        answer: 'Use the available edit or delete actions on the Properties and Notices pages. Property changes are subject to the lease and occupancy rules shown by the app.',
      },
    ],
  },
  {
    title: 'For tenants',
    questions: [
      {
        question: 'Where can I find my tenant ID?',
        answer: 'Your tenant ID appears on the My home page. Copy it or use the sharing options to send it to your property owner so they can connect your lease.',
      },
      {
        question: 'Where can I view my lease and property information?',
        answer: 'Open My home and select an active lease card to see its details, rent terms, amenities, and property owner contact information.',
      },
      {
        question: 'How can I see what rent I owe?',
        answer: 'Open Rent to see your current balance, due dates, outstanding rent items, and payment history.',
      },
      {
        question: 'How do I pay rent online?',
        answer: 'On the Rent page, choose Pay for an eligible outstanding rent item and confirm the payment.',
      },
      {
        question: 'How do I report a cash rent payment?',
        answer: 'Choose Cash claim for an eligible outstanding rent item. Your owner must review and approve the claim before it is marked paid.',
      },
      {
        question: 'Why was a late fine added to my rent?',
        answer: 'A lease can specify grace days and a fine per day. If rent remains unpaid after the grace period, the applicable fine is included in the balance.',
      },
      {
        question: 'How do I report a repair or maintenance issue?',
        answer: 'Open Maintenance and submit a request with a title, description, and priority. You can open the request later to follow its status and add comments.',
      },
      {
        question: 'Can I follow up on a maintenance request?',
        answer: 'Yes. Open the request from Maintenance to view its status and comments, and add a comment to continue the conversation with your owner.',
      },
      {
        question: 'Where can I read announcements from my owner?',
        answer: 'Open Notices to browse announcements. Recent notices may also appear on your My home page.',
      },
    ],
  },
  {
    title: 'Payments and account questions',
    questions: [
      {
        question: 'Why is my lease or property not showing?',
        answer: 'A property appears in your tenant workspace after an owner connects it to your tenant ID with a lease. If it is missing, confirm that the owner has your current tenant ID and has linked the lease.',
      },
      {
        question: 'Why is a cash payment still awaiting approval?',
        answer: 'A cash claim is not final until the property owner reviews it. Contact your owner if it remains awaiting approval.',
      },
      {
        question: 'What should I do if a page shows an error?',
        answer: 'Try refreshing the page and confirm you are signed in to the correct owner or tenant account. If the issue continues, contact your workspace administrator and include the page and error message.',
      },
      {
        question: 'How do I sign out?',
        answer: 'Choose the sign-out button in the top bar and confirm. You can sign in again with your account credentials.',
      },
    ],
  },
]

export function FaqPage() {
  const [openQuestion, setOpenQuestion] = useState<string | null>(null)

  return <section className="page-section faq-page">
    <PageHeading eyebrow="HELP CENTER" title="Frequently asked questions" subtitle="Quick answers about managing your rental workspace." />
    <div className="faq-sections">
      {faqSections.map((section, sectionIndex) => (
        <section className="faq-section surface" key={section.title}>
          <h2>{section.title}</h2>
          <div className="faq-list">
            {section.questions.map(({ question, answer }, questionIndex) => {
              const id = `faq-${sectionIndex}-${questionIndex}`
              const isOpen = openQuestion === id
              return <article className={`faq-item ${isOpen ? 'faq-item-open' : ''}`} key={question}>
                <h3>
                  <button type="button" className="faq-question" aria-expanded={isOpen} aria-controls={`${id}-answer`} onClick={() => setOpenQuestion(isOpen ? null : id)}>
                    <span>{question}</span>
                    <ChevronDown size={18} aria-hidden="true" />
                  </button>
                </h3>
                {isOpen && <div className="faq-answer" id={`${id}-answer`}><p>{answer}</p></div>}
              </article>
            })}
          </div>
        </section>
      ))}
    </div>
  </section>
}
