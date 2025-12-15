import { Routes, Route } from 'react-router-dom'

function App() {
  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <h1 className="text-3xl font-bold text-gray-900">LookMarket</h1>
          <p className="text-gray-600">패션 통합 커머스 플랫폼</p>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Routes>
          <Route path="/" element={<HomePage />} />
        </Routes>
      </main>

      <footer className="bg-white shadow mt-8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <p className="text-center text-gray-500 text-sm">
            © 2024 LookMarket. Java 21 + Spring Boot 3.3 + React 18
          </p>
        </div>
      </footer>
    </div>
  )
}

function HomePage() {
  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h2 className="text-2xl font-bold text-gray-900 mb-4">
        Welcome to LookMarket
      </h2>
      <p className="text-gray-600 mb-4">
        무신사, 올리브영 스타일의 멀티 브랜드 패션/뷰티 통합 플랫폼
      </p>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <FeatureCard
          title="Java 21"
          description="Virtual Threads 활용"
          icon="☕"
        />
        <FeatureCard
          title="Spring Boot 3.3"
          description="최신 프레임워크"
          icon="🍃"
        />
        <FeatureCard
          title="Kafka"
          description="이벤트 기반 아키텍처"
          icon="📨"
        />
        <FeatureCard
          title="Elasticsearch"
          description="고성능 검색"
          icon="🔍"
        />
      </div>
    </div>
  )
}

interface FeatureCardProps {
  title: string
  description: string
  icon: string
}

function FeatureCard({ title, description, icon }: FeatureCardProps) {
  return (
    <div className="bg-gray-50 rounded-lg p-4 text-center">
      <div className="text-4xl mb-2">{icon}</div>
      <h3 className="font-bold text-lg text-gray-900">{title}</h3>
      <p className="text-sm text-gray-600">{description}</p>
    </div>
  )
}

export default App
