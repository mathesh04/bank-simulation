import { useEffect, useMemo, useState } from 'react'
import { api } from './services/api'
import { clearSession, currentUsername, currentUserRole, isAdmin, isLoggedIn, saveSession } from './utils/session'

const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' })

function AuthScreen({ onAuthenticated }) {
  const [mode, setMode] = useState('login')
  const [form, setForm] = useState({ username: '', password: '' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function submit(e) {
    e.preventDefault()
    setBusy(true); setError('')
    try {
      if (mode === 'register') {
        await api.register(form)
      }
      const auth = await api.login(form)
      saveSession(auth.token, auth.username, auth.role)
      onAuthenticated()
    } catch (err) {
      setError(err.message)
    } finally { setBusy(false) }
  }

  return (
    <main className="auth-shell">
      <section className="auth-card">
        <div className="brand-mark">₹</div>
        <p className="eyebrow"> BANKING SIMULATION</p>
        <h1>{mode === 'login' ? 'Welcome back' : 'Create your account'}</h1>
        <p className="muted">Simulated banking platform.</p>
        <form onSubmit={submit} className="stack">
          <label>Username<input autoFocus value={form.username} onChange={e => setForm({...form, username:e.target.value})} placeholder="e.g. username" required /></label>
          <label>Password<input type="password" value={form.password} onChange={e => setForm({...form, password:e.target.value})} placeholder="Minimum 6 characters" required /></label>
          {error && <div className="alert error">{error}</div>}
          <button className="primary wide" disabled={busy}>{busy ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Register & sign in'}</button>
        </form>
        <button className="link-button" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError('') }}>
          {mode === 'login' ? 'New customer? Create an account' : 'Already registered? Sign in'}
        </button>
        <div className="security-note">
          Default administrator account: <b>admin</b> / <b>Admin@123456</b><br/>
          Secure session protected via signed JWT tokens.
        </div>
      </section>
    </main>
  )
}

function Modal({ title, children, onClose }) {
  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <div className="modal" onMouseDown={e => e.stopPropagation()}>
        <div className="modal-head"><h2>{title}</h2><button className="icon-button" onClick={onClose}>×</button></div>
        {children}
      </div>
    </div>
  )
}

function StatusBadge({ status }) {
  const s = (status || 'ACTIVE').toUpperCase()
  let cls = 'status-active'
  if (s === 'FROZEN') cls = 'status-frozen'
  if (s === 'CLOSED') cls = 'status-closed'
  if (s === 'SUSPENDED') cls = 'status-suspended'
  return <span className={`status-pill ${cls}`}>{s}</span>
}


function CustomerDashboard({ accounts, selected, setSelected, transactions, loadAccounts, loadTransactions, flash }) {
  const [modal, setModal] = useState(null)
  const total = useMemo(() => accounts.reduce((sum, a) => sum + Number(a.balance), 0), [accounts])
  const recent = transactions.slice(0, 10)

  async function action(kind, values) {
    try {
      if (kind === 'deposit') await api.deposit(values.accNo, values.amount)
      if (kind === 'withdraw') await api.withdraw(values.accNo, values.amount)
      if (kind === 'transfer') await api.transfer(values.fromAccNo, values.toAccNo, values.amount, values.recipientUsername)
      if (kind === 'create') await api.createAccount(values.type)
      if (kind === 'close') await api.closeAccount(values.accNo)

      setModal(null)
      await loadAccounts()
      if (selected) await loadTransactions(selected.accNo)
      flash('Operation completed successfully.')
    } catch (err) {
      flash(err.message, 'error')
    }
  }

  const isSelectedFrozen = selected?.status === 'FROZEN'
  const isSelectedClosed = selected?.status === 'CLOSED'

  return (
    <div className="customer-view">
      <div className="page-heading">
        <div>
          <p className="eyebrow">CUSTOMER PORTAL</p>
          <h1>Overview & Accounts</h1>
          <p className="muted">Manage your simulated deposits, withdrawals, and account policies.</p>
        </div>
        <button className="primary" onClick={() => setModal('create')}>+ Open new account</button>
      </div>

      {isSelectedFrozen && (
        <div className="alert warning">
          <span>⚠️ <b>Account #{selected.accNo} is FROZEN by Bank Administration.</b> You may deposit funds, but withdrawals and outbound transfers are blocked.</span>
        </div>
      )}

      {isSelectedClosed && (
        <div className="alert error">
          <span>🚫 <b>Account #{selected.accNo} is CLOSED.</b> All operations are permanently disabled.</span>
        </div>
      )}

      <section className="summary-grid">
        <div className="summary-card accent">
          <span>Total portfolio balance</span>
          <strong>{money.format(total)}</strong>
          <small>Across {accounts.length} account{accounts.length !== 1 ? 's' : ''}</small>
        </div>
        <div className="summary-card">
          <span>Accounts overview</span>
          <strong>{accounts.length}</strong>
          <small>{accounts.filter(a => a.status === 'ACTIVE').length} Active · {accounts.filter(a => a.status === 'FROZEN').length} Frozen</small>
        </div>
        <div className="summary-card">
          <span>Selected account</span>
          <strong>{selected ? `#${selected.accNo}` : '—'}</strong>
          <small>
            {selected ? (
              <>
                {selected.type} · <StatusBadge status={selected.status} />
              </>
            ) : 'Select an account below'}
          </small>
        </div>
      </section>

      <section className="content-grid">
        <div className="panel">
          <div className="panel-head">
            <div>
              <h2>Your accounts</h2>
              <p className="muted">Click an account to inspect details and transactions.</p>
            </div>
          </div>
          {accounts.length === 0 ? (
            <div className="empty">No accounts yet. Open a Savings or Checking account above.</div>
          ) : (
            <div className="account-list">
              {accounts.map(account => {
                const isSel = selected?.accNo === account.accNo
                const isFrz = account.status === 'FROZEN'
                return (
                  <button
                    key={account.accNo}
                    className={`account-row ${isSel ? 'selected' : ''} ${isFrz ? 'frozen-card' : ''}`}
                    onClick={() => setSelected(account)}
                  >
                    <div className="account-icon">{account.type === 'SAVINGS' ? '₹S' : '₹C'}</div>
                    <div className="account-main">
                      <div className="account-tags">
                        <strong>{account.type}</strong>
                        <StatusBadge status={account.status} />
                        <span className="policy-chip">
                          {account.type === 'SAVINGS' ? 'Min ₹500' : 'Overdraft ₹5,000'}
                        </span>
                      </div>
                      <span>Account #{account.accNo}</span>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <strong style={{ display: 'block', fontSize: '15px' }}>{money.format(account.balance)}</strong>
                    </div>
                    <span className="chevron">›</span>
                  </button>
                )
              })}
            </div>
          )}
        </div>

        <div className="panel">
          <div className="panel-head">
            <div>
              <h2>Banking actions</h2>
              <p className="muted">Quick transactional operations adhering to OOP policies.</p>
            </div>
          </div>
          <div className="action-grid">
            <button
              onClick={() => selected ? setModal('deposit') : flash('Select an account first.', 'error')}
              disabled={!selected || isSelectedClosed}
            >
              <span className="action-icon">↓</span>
              <b>Deposit</b>
              <small>Allowed on Active & Frozen</small>
            </button>

            <button
              onClick={() => selected ? setModal('withdraw') : flash('Select an account first.', 'error')}
              disabled={!selected || isSelectedFrozen || isSelectedClosed}
            >
              <span className="action-icon">↑</span>
              <b>Withdraw</b>
              <small>{isSelectedFrozen ? 'Blocked (Frozen)' : 'Adheres to account policy'}</small>
            </button>

            <button
              onClick={() => selected ? setModal('transfer') : flash('Select an account first.', 'error')}
              disabled={!selected || isSelectedFrozen || isSelectedClosed}
            >
              <span className="action-icon">↔</span>
              <b>Transfer</b>
              <small>To another user or self</small>
            </button>

            <button onClick={() => setModal('create')}>
              <span className="action-icon">+</span>
              <b>New account</b>
              <small>Savings or Checking</small>
            </button>
          </div>

          {selected && selected.status === 'ACTIVE' && Number(selected.balance) === 0 && (
            <div style={{ marginTop: '16px', textAlign: 'center' }}>
              <button className="close-btn-ghost" onClick={() => action('close', { accNo: selected.accNo })}>
                Close account #{selected.accNo} (Balance is ₹0.00)
              </button>
            </div>
          )}
        </div>
      </section>

      <section className="panel transactions">
        <div className="panel-head">
          <div>
            <h2>Transaction history</h2>
            <p className="muted">{selected ? `Activity statement for Account #${selected.accNo}` : 'Select an account'}</p>
          </div>
        </div>
        {!selected ? (
          <div className="empty">Select an account above to view statement.</div>
        ) : recent.length === 0 ? (
          <div className="empty">No recorded transactions for this account yet.</div>
        ) : (
          <div className="transaction-list">
            {recent.map(t => {
              const isOut = t.txnType.includes('OUT') || t.txnType === 'WITHDRAW'
              return (
                <div className="transaction-row" key={t.txnId}>
                  <div className={`txn-dot ${isOut ? 'out' : 'in'}`}>{isOut ? '↑' : '↓'}</div>
                  <div className="txn-main">
                    <strong>{t.txnType.replaceAll('_', ' ')}</strong>
                    <span>{new Date(t.txnDate).toLocaleString('en-IN')} · Txn #{t.txnId}</span>
                  </div>
                  <strong className={isOut ? 'negative' : 'positive'}>
                    {isOut ? '-' : '+'}{money.format(t.amount)}
                  </strong>
                </div>
              )
            })}
          </div>
        )}
      </section>

      {modal === 'create' && (
        <Modal title="Open new account" onClose={() => setModal(null)}>
          <AccountForm onSubmit={v => action('create', v)} />
        </Modal>
      )}
      {modal === 'deposit' && (
        <Modal title={`Deposit into Account #${selected?.accNo}`} onClose={() => setModal(null)}>
          <AmountForm label="Deposit amount (₹)" onSubmit={v => action('deposit', { accNo: selected.accNo, amount: v.amount })} />
        </Modal>
      )}
      {modal === 'withdraw' && (
        <Modal title={`Withdraw from Account #${selected?.accNo}`} onClose={() => setModal(null)}>
          <AmountForm
            label={`Withdrawal amount (₹) — Current balance: ${money.format(selected?.balance)}`}
            onSubmit={v => action('withdraw', { accNo: selected.accNo, amount: v.amount })}
          />
        </Modal>
      )}
      {modal === 'transfer' && (
        <Modal title="Inter-Account Transfer" onClose={() => setModal(null)}>
          <TransferForm accounts={accounts} selected={selected} onSubmit={v => action('transfer', v)} />
        </Modal>
      )}
    </div>
  )
}


function AdminPortal({ flash }) {
  const [subTab, setSubTab] = useState('overview')
  const [stats, setStats] = useState(null)
  const [users, setUsers] = useState([])
  const [accounts, setAccounts] = useState([])
  const [activities, setActivities] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(false)

  async function loadData() {
    setLoading(true)
    try {
      const [s, u, a, act] = await Promise.all([
        api.admin.stats(),
        api.admin.users(),
        api.admin.accounts(),
        api.admin.activities()
      ])
      setStats(s)
      setUsers(u)
      setAccounts(a)
      setActivities(act)
    } catch (err) {
      flash(err.message, 'error')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  async function toggleUserStatus(userId, currentStatus) {
    const nextStatus = currentStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'
    try {
      await api.admin.updateUserStatus(userId, nextStatus)
      flash(`User status changed to ${nextStatus}.`)
      loadData()
    } catch (err) {
      flash(err.message, 'error')
    }
  }

  async function toggleUserRole(userId, currentRole) {
    const nextRole = currentRole === 'ROLE_ADMIN' ? 'ROLE_CUSTOMER' : 'ROLE_ADMIN'
    try {
      await api.admin.updateUserRole(userId, nextRole)
      flash(`User role changed to ${nextRole}.`)
      loadData()
    } catch (err) {
      flash(err.message, 'error')
    }
  }

  async function toggleAccountStatus(accNo, targetStatus) {
    try {
      await api.admin.updateAccountStatus(accNo, targetStatus)
      flash(`Account #${accNo} status changed to ${targetStatus}.`)
      loadData()
    } catch (err) {
      flash(err.message, 'error')
    }
  }

  const filteredUsers = useMemo(() => {
    if (!search) return users
    const q = search.toLowerCase()
    return users.filter(u => u.username.toLowerCase().includes(q) || String(u.userId).includes(q))
  }, [users, search])

  const filteredAccounts = useMemo(() => {
    if (!search) return accounts
    const q = search.toLowerCase()
    return accounts.filter(a =>
      String(a.accNo).includes(q) ||
      (a.username && a.username.toLowerCase().includes(q)) ||
      a.type.toLowerCase().includes(q) ||
      a.status.toLowerCase().includes(q)
    )
  }, [accounts, search])

  const filteredActivities = useMemo(() => {
    if (!search) return activities
    const q = search.toLowerCase()
    return activities.filter(act =>
      String(act.txnId).includes(q) ||
      (act.username && act.username.toLowerCase().includes(q)) ||
      String(act.accNo).includes(q) ||
      act.txnType.toLowerCase().includes(q)
    )
  }, [activities, search])

  return (
    <div className="admin-view">
      <div className="page-heading">
        <div>
          <p className="eyebrow">ADMINISTRATION CONTROL SUITE</p>
          <h1>Bank Management & Activity Audit</h1>
          <p className="muted">Manage user roles, account lifecycle states, and monitor real-time financial ledger activity.</p>
        </div>
        <button className="ghost" onClick={loadData} disabled={loading}>
          {loading ? 'Refreshing…' : '↻ Refresh Data'}
        </button>
      </div>

      <nav className="admin-subnav">
        <button className={`subnav-tab ${subTab === 'overview' ? 'active' : ''}`} onClick={() => { setSubTab('overview'); setSearch('') }}>Overview & Metrics</button>
        <button className={`subnav-tab ${subTab === 'users' ? 'active' : ''}`} onClick={() => { setSubTab('users'); setSearch('') }}>User Management ({users.length})</button>
        <button className={`subnav-tab ${subTab === 'accounts' ? 'active' : ''}`} onClick={() => { setSubTab('accounts'); setSearch('') }}>Account Supervision ({accounts.length})</button>
        <button className={`subnav-tab ${subTab === 'activities' ? 'active' : ''}`} onClick={() => { setSubTab('activities'); setSearch('') }}>Global Activity Log ({activities.length})</button>
      </nav>


      {subTab === 'overview' && stats && (
        <section>
          <div className="admin-stat-grid">
            <div className="stat-box">
              <span>Total registered users</span>
              <strong>{stats.totalUsers}</strong>
              <small>Customers & Administrators</small>
            </div>
            <div className="stat-box">
              <span>Total bank accounts</span>
              <strong>{stats.totalAccounts}</strong>
              <small>{stats.activeAccounts} Active · {stats.frozenAccounts} Frozen · {stats.closedAccounts} Closed</small>
            </div>
            <div className="stat-box">
              <span>System active liquidity</span>
              <strong style={{ color: '#047857' }}>{money.format(stats.totalSystemBalance)}</strong>
              <small>Total active customer balances</small>
            </div>
            <div className="stat-box">
              <span>Completed transactions</span>
              <strong>{stats.totalTransactions}</strong>
              <small>All deposits, withdrawals, transfers</small>
            </div>
          </div>

          <div className="content-grid">
            <div className="panel">
              <div className="panel-head">
                <h2>Quick summary</h2>
              </div>
              <p className="muted" style={{ lineHeight: '1.6' }}>
                The banking simulation runs with <b>strong OOP domain modeling</b>.
                Savings accounts enforce a minimum balance of <b>₹500.00</b>.
                Checking accounts provide an overdraft buffer up to <b>₹5,000.00</b>.
                Frozen accounts permit credit deposits while blocking withdrawals and transfers.
              </p>
            </div>
            <div className="panel">
              <div className="panel-head">
                <h2>Recent system transactions</h2>
              </div>
              <div className="transaction-list">
                {activities.slice(0, 5).map(act => (
                  <div key={act.txnId} className="transaction-row">
                    <div className="txn-dot in">₹</div>
                    <div className="txn-main">
                      <strong>{act.username} · Account #{act.accNo}</strong>
                      <span>{act.txnType} · {new Date(act.txnDate).toLocaleTimeString('en-IN')}</span>
                    </div>
                    <strong>{money.format(act.amount)}</strong>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>
      )}


      {subTab === 'users' && (
        <section className="panel">
          <div className="filter-bar">
            <input
              placeholder="Search users by username or ID…"
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
          </div>
          <div className="data-table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>User ID</th>
                  <th>Username</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Accounts</th>
                  <th>Total Balance</th>
                  <th>Registered</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.length === 0 ? (
                  <tr><td colSpan="8" className="empty">No matching users found.</td></tr>
                ) : filteredUsers.map(u => (
                  <tr key={u.userId}>
                    <td>#{u.userId}</td>
                    <td><b>{u.username}</b></td>
                    <td>
                      <span className={`badge-role ${u.role === 'ROLE_ADMIN' ? 'badge-admin' : 'badge-customer'}`}>
                        {u.role === 'ROLE_ADMIN' ? 'ADMIN' : 'CUSTOMER'}
                      </span>
                    </td>
                    <td><StatusBadge status={u.status} /></td>
                    <td>{u.accountCount}</td>
                    <td><b>{money.format(u.totalBalance)}</b></td>
                    <td>{new Date(u.createdAt).toLocaleDateString('en-IN')}</td>
                    <td>
                      <div className="table-actions">
                        <button
                          className={`action-btn-sm ${u.status === 'ACTIVE' ? 'warn' : 'success'}`}
                          onClick={() => toggleUserStatus(u.userId, u.status)}
                        >
                          {u.status === 'ACTIVE' ? 'Suspend' : 'Activate'}
                        </button>
                        <button
                          className="action-btn-sm"
                          onClick={() => toggleUserRole(u.userId, u.role)}
                        >
                          {u.role === 'ROLE_ADMIN' ? 'Demote to Customer' : 'Promote to Admin'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}


      {subTab === 'accounts' && (
        <section className="panel">
          <div className="filter-bar">
            <input
              placeholder="Search accounts by #, owner, or status…"
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
          </div>
          <div className="data-table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Acc #</th>
                  <th>Owner</th>
                  <th>Type</th>
                  <th>Policy Details</th>
                  <th>Balance</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredAccounts.length === 0 ? (
                  <tr><td colSpan="7" className="empty">No matching accounts found.</td></tr>
                ) : filteredAccounts.map(a => (
                  <tr key={a.accNo}>
                    <td><b>#{a.accNo}</b></td>
                    <td>{a.username}</td>
                    <td><b>{a.type}</b></td>
                    <td>
                      <span className="policy-chip">
                        {a.type === 'SAVINGS' ? `Min. Balance: ${money.format(a.minimumBalance || 500)}` : `Overdraft Limit: ${money.format(a.overdraftLimit || 5000)}`}
                      </span>
                    </td>
                    <td><b style={{ color: Number(a.balance) < 0 ? '#b91c1c' : '#047857' }}>{money.format(a.balance)}</b></td>
                    <td><StatusBadge status={a.status} /></td>
                    <td>
                      <div className="table-actions">
                        {a.status === 'ACTIVE' && (
                          <button
                            className="action-btn-sm warn"
                            onClick={() => toggleAccountStatus(a.accNo, 'FROZEN')}
                          >
                            Freeze Account
                          </button>
                        )}
                        {a.status === 'FROZEN' && (
                          <button
                            className="action-btn-sm success"
                            onClick={() => toggleAccountStatus(a.accNo, 'ACTIVE')}
                          >
                            Unfreeze Account
                          </button>
                        )}
                        {a.status !== 'CLOSED' && Number(a.balance) === 0 && (
                          <button
                            className="action-btn-sm danger"
                            onClick={() => toggleAccountStatus(a.accNo, 'CLOSED')}
                          >
                            Close
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}


      {subTab === 'activities' && (
        <section className="panel">
          <div className="filter-bar">
            <input
              placeholder="Search activity by user, account, or type…"
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
          </div>
          <div className="data-table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Txn #</th>
                  <th>Date & Time</th>
                  <th>User</th>
                  <th>Account</th>
                  <th>Acc Type</th>
                  <th>Acc Status</th>
                  <th>Operation Type</th>
                  <th>Amount</th>
                </tr>
              </thead>
              <tbody>
                {filteredActivities.length === 0 ? (
                  <tr><td colSpan="8" className="empty">No matching system activities found.</td></tr>
                ) : filteredActivities.map(act => {
                  const isOut = act.txnType.includes('OUT') || act.txnType === 'WITHDRAW'
                  return (
                    <tr key={act.txnId}>
                      <td>#{act.txnId}</td>
                      <td>{new Date(act.txnDate).toLocaleString('en-IN')}</td>
                      <td><b>{act.username}</b></td>
                      <td>#{act.accNo}</td>
                      <td>{act.accountType}</td>
                      <td><StatusBadge status={act.accountStatus} /></td>
                      <td><b>{act.txnType.replaceAll('_', ' ')}</b></td>
                      <td>
                        <strong className={isOut ? 'negative' : 'positive'}>
                          {isOut ? '-' : '+'}{money.format(act.amount)}
                        </strong>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </div>
  )
}

function AccountForm({ onSubmit }) {
  const [type, setType] = useState('SAVINGS')
  return (
    <form className="stack" onSubmit={e => { e.preventDefault(); onSubmit({ type }) }}>
      <label>
        Account type
        <select value={type} onChange={e => setType(e.target.value)}>
          <option value="SAVINGS">SAVINGS (Min. balance ₹500, no overdraft)</option>
          <option value="CHECKING">CHECKING (₹0 min balance, ₹5,000 overdraft limit)</option>
        </select>
      </label>
      <button className="primary wide">Open account</button>
    </form>
  )
}

function AmountForm({ label, onSubmit }) {
  const [amount, setAmount] = useState('')
  return (
    <form className="stack" onSubmit={e => { e.preventDefault(); onSubmit({ amount }) }}>
      <label>
        {label}
        <input type="number" min="0.01" step="0.01" value={amount} onChange={e => setAmount(e.target.value)} placeholder="0.00" required />
      </label>
      <button className="primary wide">Confirm</button>
    </form>
  )
}

function TransferForm({ accounts, selected, onSubmit }) {
  const [mode, setMode] = useState('other') // 'other' | 'self'
  const [fromAccNo, setFrom] = useState(selected?.accNo || accounts[0]?.accNo)
  const [toAccNo, setTo] = useState(accounts.find(a => a.accNo !== fromAccNo)?.accNo || '')
  const [amount, setAmount] = useState('')


  const [searchQuery, setSearchQuery] = useState('')
  const [recipients, setRecipients] = useState([])
  const [loadingRecipients, setLoadingRecipients] = useState(false)
  const [selectedRecipient, setSelectedRecipient] = useState(null)
  const [selectedRecipientAccount, setSelectedRecipientAccount] = useState(null)

  useEffect(() => {
    let active = true
    async function fetchRecipients() {
      setLoadingRecipients(true)
      try {
        const res = await api.searchRecipients(searchQuery)
        if (active) setRecipients(res || [])
      } catch (err) {
        console.error('Failed to search recipients', err)
      } finally {
        if (active) setLoadingRecipients(false)
      }
    }
    const timer = setTimeout(fetchRecipients, 250)
    return () => { active = false; clearTimeout(timer) }
  }, [searchQuery])

  function handleSelectRecipientAccount(user, acc) {
    setSelectedRecipient(user)
    setSelectedRecipientAccount(acc)
  }

  function handleClearRecipient() {
    setSelectedRecipient(null)
    setSelectedRecipientAccount(null)
  }

  function handleSubmit(e) {
    e.preventDefault()
    if (mode === 'other') {
      if (!selectedRecipientAccount && !searchQuery.trim()) {
        alert('Please search for and select a recipient user account.')
        return
      }
      onSubmit({
        fromAccNo,
        toAccNo: selectedRecipientAccount ? selectedRecipientAccount.accNo : null,
        recipientUsername: selectedRecipient ? selectedRecipient.username : searchQuery.trim(),
        amount
      })
    } else {
      if (!toAccNo) {
        alert('Please select a destination account.')
        return
      }
      onSubmit({
        fromAccNo,
        toAccNo: Number(toAccNo),
        amount
      })
    }
  }

  const fromAccountObj = accounts.find(a => a.accNo === Number(fromAccNo))

  return (
    <form className="stack" onSubmit={handleSubmit}>
      <div className="transfer-modes">
        <button
          type="button"
          className={`mode-btn ${mode === 'other' ? 'active' : ''}`}
          onClick={() => setMode('other')}
        >
          To another user
        </button>
        <button
          type="button"
          className={`mode-btn ${mode === 'self' ? 'active' : ''}`}
          onClick={() => setMode('self')}
          disabled={accounts.length < 2}
        >
          Between my own accounts
        </button>
      </div>

      <label>
        From account
        <select value={fromAccNo} onChange={e => setFrom(Number(e.target.value))}>
          {accounts.map(a => (
            <option key={a.accNo} value={a.accNo} disabled={a.status === 'FROZEN' || a.status === 'CLOSED'}>
              #{a.accNo} · {a.type} · {money.format(a.balance)} {a.status === 'FROZEN' ? '(FROZEN)' : ''}
            </option>
          ))}
        </select>
        {fromAccountObj && (
          <small className="muted" style={{ fontSize: '11px' }}>
            Available balance: <b>{money.format(fromAccountObj.balance)}</b>
          </small>
        )}
      </label>

      {mode === 'other' ? (
        <div className="recipient-search-wrap">
          <label>Recipient user</label>
          {selectedRecipient ? (
            <div className="selected-recipient-banner">
              <div className="selected-recipient-info">
                <strong>@{selectedRecipient.username}</strong>
                <small>Account #{selectedRecipientAccount?.accNo} · {selectedRecipientAccount?.type}</small>
              </div>
              <button type="button" className="change-recipient-btn" onClick={handleClearRecipient}>
                Change
              </button>
            </div>
          ) : (
            <>
              <input
                placeholder="Search recipient by username (e.g. admin, mathesh)..."
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                autoFocus
              />
              <div className="recipient-list">
                {loadingRecipients ? (
                  <div className="empty" style={{ padding: '15px' }}>Searching bank users…</div>
                ) : recipients.length === 0 ? (
                  <div className="empty" style={{ padding: '15px' }}>
                    {searchQuery ? `No active user matching "${searchQuery}".` : 'No other users found in the system.'}
                  </div>
                ) : (
                  recipients.map(user => (
                    <div key={user.userId} className="recipient-card">
                      <div className="recipient-card-head">
                        <span className="recipient-username">@{user.username}</span>
                        <small className="muted">{user.accounts?.length || 0} open account(s)</small>
                      </div>
                      <div className="recipient-accounts-row">
                        {(!user.accounts || user.accounts.length === 0) ? (
                          <span style={{ fontSize: '11px', color: '#94a3b8' }}>No active accounts</span>
                        ) : (
                          user.accounts.map(acc => (
                            <button
                              type="button"
                              key={acc.accNo}
                              className="account-pick-btn"
                              onClick={() => handleSelectRecipientAccount(user, acc)}
                            >
                              Send to #{acc.accNo} ({acc.type})
                            </button>
                          ))
                        )}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </>
          )}
        </div>
      ) : (
        <label>
          To my account
          <select value={toAccNo} onChange={e => setTo(Number(e.target.value))}>
            {accounts.filter(a => a.accNo !== Number(fromAccNo)).map(a => (
              <option key={a.accNo} value={a.accNo}>
                #{a.accNo} · {a.type} · {money.format(a.balance)} {a.status === 'FROZEN' ? '(FROZEN)' : ''}
              </option>
            ))}
          </select>
        </label>
      )}

      <label>
        Transfer amount (₹)
        <input
          type="number"
          min="0.01"
          step="0.01"
          value={amount}
          onChange={e => setAmount(e.target.value)}
          placeholder="0.00"
          required
        />
      </label>

      <button
        className="primary wide"
        disabled={mode === 'other' && !selectedRecipient && !searchQuery.trim()}
      >
        {mode === 'other' && selectedRecipient
          ? `Transfer ${amount ? money.format(amount) : ''} to @${selectedRecipient.username}`
          : 'Execute Transfer'}
      </button>
    </form>
  )
}

function Dashboard({ onLogout }) {
  const [view, setView] = useState('customer') // 'customer' | 'admin'
  const [accounts, setAccounts] = useState([])
  const [selected, setSelected] = useState(null)
  const [transactions, setTransactions] = useState([])
  const [notice, setNotice] = useState(null)
  const [loading, setLoading] = useState(true)

  const adminRole = isAdmin()

  function flash(text, type = 'success') {
    setNotice({ type, text })
    setTimeout(() => setNotice(null), 4000)
  }

  async function loadAccounts() {
    setLoading(true)
    try {
      const next = await api.accounts()
      const list = Array.isArray(next) ? next : []
      setAccounts(list)
      if (list.length > 0) {
        if (!selected) {
          setSelected(list[0])
        } else {
          const fresh = list.find(a => a.accNo === selected.accNo)
          setSelected(fresh || list[0])
        }
      } else {
        setSelected(null)
      }
      return list
    } catch (err) {
      flash(err.message, 'error')
      throw err
    } finally {
      setLoading(false)
    }
  }

  async function loadTransactions(accNo) {
    if (!accNo) return
    try {
      setTransactions(await api.transactions(accNo))
    } catch (err) {
      flash(err.message, 'error')
    }
  }

  useEffect(() => {
    loadAccounts()
  }, [])

  useEffect(() => {
    if (selected?.accNo) {
      loadTransactions(selected.accNo)
    }
  }, [selected?.accNo])

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="topbar-inner">
          <div className="brand">
            <span className="brand-mini">₹</span>
            <span>Banking Simulation</span>
          </div>

          {adminRole && (
            <div className="view-switcher">
              <button
                className={`view-tab ${view === 'customer' ? 'active' : ''}`}
                onClick={() => setView('customer')}
              >
                Customer Portal
              </button>
              <button
                className={`view-tab ${view === 'admin' ? 'active' : ''}`}
                onClick={() => setView('admin')}
              >
                Admin Suite
              </button>
            </div>
          )}

          <div className="top-actions">
            <span className="user-chip">
              {currentUsername()}
              <span className={`badge-role ${adminRole ? 'badge-admin' : 'badge-customer'}`}>
                {adminRole ? 'ADMIN' : 'CUSTOMER'}
              </span>
            </span>
            <button className="ghost" onClick={() => { clearSession(); onLogout() }}>
              Sign out
            </button>
          </div>
        </div>
      </header>

      <main className="dashboard">
        {notice && (
          <div className={`alert ${notice.type}`}>
            <span>{notice.text}</span>
            <button className="icon-button" style={{ width: 22, height: 22, fontSize: 14 }} onClick={() => setNotice(null)}>×</button>
          </div>
        )}

        {view === 'admin' && adminRole ? (
          <AdminPortal flash={flash} />
        ) : (
          <CustomerDashboard
            accounts={accounts}
            selected={selected}
            setSelected={setSelected}
            transactions={transactions}
            loadAccounts={loadAccounts}
            loadTransactions={loadTransactions}
            flash={flash}
          />
        )}
      </main>
    </div>
  )
}

export default function App() {
  const [authenticated, setAuthenticated] = useState(isLoggedIn())
  return authenticated
    ? <Dashboard onLogout={() => setAuthenticated(false)} />
    : <AuthScreen onAuthenticated={() => setAuthenticated(true)} />
}
