// Generated by the gRPC C++ plugin.
// If you make any local change, they will be lost.
// source: JetToCpp.proto
#ifndef GRPC_JetToCpp_2eproto__INCLUDED
#define GRPC_JetToCpp_2eproto__INCLUDED

#include "JetToCpp.pb.h"

#include <functional>
#include <grpcpp/generic/async_generic_service.h>
#include <grpcpp/support/async_stream.h>
#include <grpcpp/support/async_unary_call.h>
#include <grpcpp/support/client_callback.h>
#include <grpcpp/client_context.h>
#include <grpcpp/completion_queue.h>
#include <grpcpp/support/message_allocator.h>
#include <grpcpp/support/method_handler.h>
#include <grpcpp/impl/proto_utils.h>
#include <grpcpp/impl/rpc_method.h>
#include <grpcpp/support/server_callback.h>
#include <grpcpp/impl/server_callback_handlers.h>
#include <grpcpp/server_context.h>
#include <grpcpp/impl/service_type.h>
#include <grpcpp/support/status.h>
#include <grpcpp/support/stub_options.h>
#include <grpcpp/support/sync_stream.h>

namespace com_hazelcast_platform_demos_banking_cva {

class JetToCpp final {
 public:
  static constexpr char const* service_full_name() {
    return "com_hazelcast_platform_demos_banking_cva.JetToCpp";
  }
  class StubInterface {
   public:
    virtual ~StubInterface() {}
    std::unique_ptr< ::grpc::ClientReaderWriterInterface< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>> streamingCall(::grpc::ClientContext* context) {
      return std::unique_ptr< ::grpc::ClientReaderWriterInterface< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>>(streamingCallRaw(context));
    }
    std::unique_ptr< ::grpc::ClientAsyncReaderWriterInterface< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>> AsyncstreamingCall(::grpc::ClientContext* context, ::grpc::CompletionQueue* cq, void* tag) {
      return std::unique_ptr< ::grpc::ClientAsyncReaderWriterInterface< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>>(AsyncstreamingCallRaw(context, cq, tag));
    }
    std::unique_ptr< ::grpc::ClientAsyncReaderWriterInterface< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>> PrepareAsyncstreamingCall(::grpc::ClientContext* context, ::grpc::CompletionQueue* cq) {
      return std::unique_ptr< ::grpc::ClientAsyncReaderWriterInterface< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>>(PrepareAsyncstreamingCallRaw(context, cq));
    }
    virtual ::grpc::Status myUnaryCall(::grpc::ClientContext* context, const ::com_hazelcast_platform_demos_banking_cva::InputMessage& request, ::com_hazelcast_platform_demos_banking_cva::OutputMessage* response) = 0;
    std::unique_ptr< ::grpc::ClientAsyncResponseReaderInterface< ::com_hazelcast_platform_demos_banking_cva::OutputMessage>> AsyncmyUnaryCall(::grpc::ClientContext* context, const ::com_hazelcast_platform_demos_banking_cva::InputMessage& request, ::grpc::CompletionQueue* cq) {
      return std::unique_ptr< ::grpc::ClientAsyncResponseReaderInterface< ::com_hazelcast_platform_demos_banking_cva::OutputMessage>>(AsyncmyUnaryCallRaw(context, request, cq));
    }
    std::unique_ptr< ::grpc::ClientAsyncResponseReaderInterface< ::com_hazelcast_platform_demos_banking_cva::OutputMessage>> PrepareAsyncmyUnaryCall(::grpc::ClientContext* context, const ::com_hazelcast_platform_demos_banking_cva::InputMessage& request, ::grpc::CompletionQueue* cq) {
      return std::unique_ptr< ::grpc::ClientAsyncResponseReaderInterface< ::com_hazelcast_platform_demos_banking_cva::OutputMessage>>(PrepareAsyncmyUnaryCallRaw(context, request, cq));
    }
    class async_interface {
     public:
      virtual ~async_interface() {}
      virtual void streamingCall(::grpc::ClientContext* context, ::grpc::ClientBidiReactor< ::com_hazelcast_platform_demos_banking_cva::InputMessage,::com_hazelcast_platform_demos_banking_cva::OutputMessage>* reactor) = 0;
      virtual void myUnaryCall(::grpc::ClientContext* context, const ::com_hazelcast_platform_demos_banking_cva::InputMessage* request, ::com_hazelcast_platform_demos_banking_cva::OutputMessage* response, std::function<void(::grpc::Status)>) = 0;
      virtual void myUnaryCall(::grpc::ClientContext* context, const ::com_hazelcast_platform_demos_banking_cva::InputMessage* request, ::com_hazelcast_platform_demos_banking_cva::OutputMessage* response, ::grpc::ClientUnaryReactor* reactor) = 0;
    };
    typedef class async_interface experimental_async_interface;
    virtual class async_interface* async() { return nullptr; }
    class async_interface* experimental_async() { return async(); }
   private:
    virtual ::grpc::ClientReaderWriterInterface< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>* streamingCallRaw(::grpc::ClientContext* context) = 0;
    virtual ::grpc::ClientAsyncReaderWriterInterface< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>* AsyncstreamingCallRaw(::grpc::ClientContext* context, ::grpc::CompletionQueue* cq, void* tag) = 0;
    virtual ::grpc::ClientAsyncReaderWriterInterface< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>* PrepareAsyncstreamingCallRaw(::grpc::ClientContext* context, ::grpc::CompletionQueue* cq) = 0;
    virtual ::grpc::ClientAsyncResponseReaderInterface< ::com_hazelcast_platform_demos_banking_cva::OutputMessage>* AsyncmyUnaryCallRaw(::grpc::ClientContext* context, const ::com_hazelcast_platform_demos_banking_cva::InputMessage& request, ::grpc::CompletionQueue* cq) = 0;
    virtual ::grpc::ClientAsyncResponseReaderInterface< ::com_hazelcast_platform_demos_banking_cva::OutputMessage>* PrepareAsyncmyUnaryCallRaw(::grpc::ClientContext* context, const ::com_hazelcast_platform_demos_banking_cva::InputMessage& request, ::grpc::CompletionQueue* cq) = 0;
  };
  class Stub final : public StubInterface {
   public:
    Stub(const std::shared_ptr< ::grpc::ChannelInterface>& channel, const ::grpc::StubOptions& options = ::grpc::StubOptions());
    std::unique_ptr< ::grpc::ClientReaderWriter< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>> streamingCall(::grpc::ClientContext* context) {
      return std::unique_ptr< ::grpc::ClientReaderWriter< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>>(streamingCallRaw(context));
    }
    std::unique_ptr<  ::grpc::ClientAsyncReaderWriter< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>> AsyncstreamingCall(::grpc::ClientContext* context, ::grpc::CompletionQueue* cq, void* tag) {
      return std::unique_ptr< ::grpc::ClientAsyncReaderWriter< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>>(AsyncstreamingCallRaw(context, cq, tag));
    }
    std::unique_ptr<  ::grpc::ClientAsyncReaderWriter< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>> PrepareAsyncstreamingCall(::grpc::ClientContext* context, ::grpc::CompletionQueue* cq) {
      return std::unique_ptr< ::grpc::ClientAsyncReaderWriter< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>>(PrepareAsyncstreamingCallRaw(context, cq));
    }
    ::grpc::Status myUnaryCall(::grpc::ClientContext* context, const ::com_hazelcast_platform_demos_banking_cva::InputMessage& request, ::com_hazelcast_platform_demos_banking_cva::OutputMessage* response) override;
    std::unique_ptr< ::grpc::ClientAsyncResponseReader< ::com_hazelcast_platform_demos_banking_cva::OutputMessage>> AsyncmyUnaryCall(::grpc::ClientContext* context, const ::com_hazelcast_platform_demos_banking_cva::InputMessage& request, ::grpc::CompletionQueue* cq) {
      return std::unique_ptr< ::grpc::ClientAsyncResponseReader< ::com_hazelcast_platform_demos_banking_cva::OutputMessage>>(AsyncmyUnaryCallRaw(context, request, cq));
    }
    std::unique_ptr< ::grpc::ClientAsyncResponseReader< ::com_hazelcast_platform_demos_banking_cva::OutputMessage>> PrepareAsyncmyUnaryCall(::grpc::ClientContext* context, const ::com_hazelcast_platform_demos_banking_cva::InputMessage& request, ::grpc::CompletionQueue* cq) {
      return std::unique_ptr< ::grpc::ClientAsyncResponseReader< ::com_hazelcast_platform_demos_banking_cva::OutputMessage>>(PrepareAsyncmyUnaryCallRaw(context, request, cq));
    }
    class async final :
      public StubInterface::async_interface {
     public:
      void streamingCall(::grpc::ClientContext* context, ::grpc::ClientBidiReactor< ::com_hazelcast_platform_demos_banking_cva::InputMessage,::com_hazelcast_platform_demos_banking_cva::OutputMessage>* reactor) override;
      void myUnaryCall(::grpc::ClientContext* context, const ::com_hazelcast_platform_demos_banking_cva::InputMessage* request, ::com_hazelcast_platform_demos_banking_cva::OutputMessage* response, std::function<void(::grpc::Status)>) override;
      void myUnaryCall(::grpc::ClientContext* context, const ::com_hazelcast_platform_demos_banking_cva::InputMessage* request, ::com_hazelcast_platform_demos_banking_cva::OutputMessage* response, ::grpc::ClientUnaryReactor* reactor) override;
     private:
      friend class Stub;
      explicit async(Stub* stub): stub_(stub) { }
      Stub* stub() { return stub_; }
      Stub* stub_;
    };
    class async* async() override { return &async_stub_; }

   private:
    std::shared_ptr< ::grpc::ChannelInterface> channel_;
    class async async_stub_{this};
    ::grpc::ClientReaderWriter< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>* streamingCallRaw(::grpc::ClientContext* context) override;
    ::grpc::ClientAsyncReaderWriter< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>* AsyncstreamingCallRaw(::grpc::ClientContext* context, ::grpc::CompletionQueue* cq, void* tag) override;
    ::grpc::ClientAsyncReaderWriter< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>* PrepareAsyncstreamingCallRaw(::grpc::ClientContext* context, ::grpc::CompletionQueue* cq) override;
    ::grpc::ClientAsyncResponseReader< ::com_hazelcast_platform_demos_banking_cva::OutputMessage>* AsyncmyUnaryCallRaw(::grpc::ClientContext* context, const ::com_hazelcast_platform_demos_banking_cva::InputMessage& request, ::grpc::CompletionQueue* cq) override;
    ::grpc::ClientAsyncResponseReader< ::com_hazelcast_platform_demos_banking_cva::OutputMessage>* PrepareAsyncmyUnaryCallRaw(::grpc::ClientContext* context, const ::com_hazelcast_platform_demos_banking_cva::InputMessage& request, ::grpc::CompletionQueue* cq) override;
    const ::grpc::internal::RpcMethod rpcmethod_streamingCall_;
    const ::grpc::internal::RpcMethod rpcmethod_myUnaryCall_;
  };
  static std::unique_ptr<Stub> NewStub(const std::shared_ptr< ::grpc::ChannelInterface>& channel, const ::grpc::StubOptions& options = ::grpc::StubOptions());

  class Service : public ::grpc::Service {
   public:
    Service();
    virtual ~Service();
    virtual ::grpc::Status streamingCall(::grpc::ServerContext* context, ::grpc::ServerReaderWriter< ::com_hazelcast_platform_demos_banking_cva::OutputMessage, ::com_hazelcast_platform_demos_banking_cva::InputMessage>* stream);
    virtual ::grpc::Status myUnaryCall(::grpc::ServerContext* context, const ::com_hazelcast_platform_demos_banking_cva::InputMessage* request, ::com_hazelcast_platform_demos_banking_cva::OutputMessage* response);
  };
  template <class BaseClass>
  class WithAsyncMethod_streamingCall : public BaseClass {
   private:
    void BaseClassMustBeDerivedFromService(const Service* /*service*/) {}
   public:
    WithAsyncMethod_streamingCall() {
      ::grpc::Service::MarkMethodAsync(0);
    }
    ~WithAsyncMethod_streamingCall() override {
      BaseClassMustBeDerivedFromService(this);
    }
    // disable synchronous version of this method
    ::grpc::Status streamingCall(::grpc::ServerContext* /*context*/, ::grpc::ServerReaderWriter< ::com_hazelcast_platform_demos_banking_cva::OutputMessage, ::com_hazelcast_platform_demos_banking_cva::InputMessage>* /*stream*/)  override {
      abort();
      return ::grpc::Status(::grpc::StatusCode::UNIMPLEMENTED, "");
    }
    void RequeststreamingCall(::grpc::ServerContext* context, ::grpc::ServerAsyncReaderWriter< ::com_hazelcast_platform_demos_banking_cva::OutputMessage, ::com_hazelcast_platform_demos_banking_cva::InputMessage>* stream, ::grpc::CompletionQueue* new_call_cq, ::grpc::ServerCompletionQueue* notification_cq, void *tag) {
      ::grpc::Service::RequestAsyncBidiStreaming(0, context, stream, new_call_cq, notification_cq, tag);
    }
  };
  template <class BaseClass>
  class WithAsyncMethod_myUnaryCall : public BaseClass {
   private:
    void BaseClassMustBeDerivedFromService(const Service* /*service*/) {}
   public:
    WithAsyncMethod_myUnaryCall() {
      ::grpc::Service::MarkMethodAsync(1);
    }
    ~WithAsyncMethod_myUnaryCall() override {
      BaseClassMustBeDerivedFromService(this);
    }
    // disable synchronous version of this method
    ::grpc::Status myUnaryCall(::grpc::ServerContext* /*context*/, const ::com_hazelcast_platform_demos_banking_cva::InputMessage* /*request*/, ::com_hazelcast_platform_demos_banking_cva::OutputMessage* /*response*/) override {
      abort();
      return ::grpc::Status(::grpc::StatusCode::UNIMPLEMENTED, "");
    }
    void RequestmyUnaryCall(::grpc::ServerContext* context, ::com_hazelcast_platform_demos_banking_cva::InputMessage* request, ::grpc::ServerAsyncResponseWriter< ::com_hazelcast_platform_demos_banking_cva::OutputMessage>* response, ::grpc::CompletionQueue* new_call_cq, ::grpc::ServerCompletionQueue* notification_cq, void *tag) {
      ::grpc::Service::RequestAsyncUnary(1, context, request, response, new_call_cq, notification_cq, tag);
    }
  };
  typedef WithAsyncMethod_streamingCall<WithAsyncMethod_myUnaryCall<Service > > AsyncService;
  template <class BaseClass>
  class WithCallbackMethod_streamingCall : public BaseClass {
   private:
    void BaseClassMustBeDerivedFromService(const Service* /*service*/) {}
   public:
    WithCallbackMethod_streamingCall() {
      ::grpc::Service::MarkMethodCallback(0,
          new ::grpc::internal::CallbackBidiHandler< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>(
            [this](
                   ::grpc::CallbackServerContext* context) { return this->streamingCall(context); }));
    }
    ~WithCallbackMethod_streamingCall() override {
      BaseClassMustBeDerivedFromService(this);
    }
    // disable synchronous version of this method
    ::grpc::Status streamingCall(::grpc::ServerContext* /*context*/, ::grpc::ServerReaderWriter< ::com_hazelcast_platform_demos_banking_cva::OutputMessage, ::com_hazelcast_platform_demos_banking_cva::InputMessage>* /*stream*/)  override {
      abort();
      return ::grpc::Status(::grpc::StatusCode::UNIMPLEMENTED, "");
    }
    virtual ::grpc::ServerBidiReactor< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>* streamingCall(
      ::grpc::CallbackServerContext* /*context*/)
      { return nullptr; }
  };
  template <class BaseClass>
  class WithCallbackMethod_myUnaryCall : public BaseClass {
   private:
    void BaseClassMustBeDerivedFromService(const Service* /*service*/) {}
   public:
    WithCallbackMethod_myUnaryCall() {
      ::grpc::Service::MarkMethodCallback(1,
          new ::grpc::internal::CallbackUnaryHandler< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>(
            [this](
                   ::grpc::CallbackServerContext* context, const ::com_hazelcast_platform_demos_banking_cva::InputMessage* request, ::com_hazelcast_platform_demos_banking_cva::OutputMessage* response) { return this->myUnaryCall(context, request, response); }));}
    void SetMessageAllocatorFor_myUnaryCall(
        ::grpc::MessageAllocator< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>* allocator) {
      ::grpc::internal::MethodHandler* const handler = ::grpc::Service::GetHandler(1);
      static_cast<::grpc::internal::CallbackUnaryHandler< ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>*>(handler)
              ->SetMessageAllocator(allocator);
    }
    ~WithCallbackMethod_myUnaryCall() override {
      BaseClassMustBeDerivedFromService(this);
    }
    // disable synchronous version of this method
    ::grpc::Status myUnaryCall(::grpc::ServerContext* /*context*/, const ::com_hazelcast_platform_demos_banking_cva::InputMessage* /*request*/, ::com_hazelcast_platform_demos_banking_cva::OutputMessage* /*response*/) override {
      abort();
      return ::grpc::Status(::grpc::StatusCode::UNIMPLEMENTED, "");
    }
    virtual ::grpc::ServerUnaryReactor* myUnaryCall(
      ::grpc::CallbackServerContext* /*context*/, const ::com_hazelcast_platform_demos_banking_cva::InputMessage* /*request*/, ::com_hazelcast_platform_demos_banking_cva::OutputMessage* /*response*/)  { return nullptr; }
  };
  typedef WithCallbackMethod_streamingCall<WithCallbackMethod_myUnaryCall<Service > > CallbackService;
  typedef CallbackService ExperimentalCallbackService;
  template <class BaseClass>
  class WithGenericMethod_streamingCall : public BaseClass {
   private:
    void BaseClassMustBeDerivedFromService(const Service* /*service*/) {}
   public:
    WithGenericMethod_streamingCall() {
      ::grpc::Service::MarkMethodGeneric(0);
    }
    ~WithGenericMethod_streamingCall() override {
      BaseClassMustBeDerivedFromService(this);
    }
    // disable synchronous version of this method
    ::grpc::Status streamingCall(::grpc::ServerContext* /*context*/, ::grpc::ServerReaderWriter< ::com_hazelcast_platform_demos_banking_cva::OutputMessage, ::com_hazelcast_platform_demos_banking_cva::InputMessage>* /*stream*/)  override {
      abort();
      return ::grpc::Status(::grpc::StatusCode::UNIMPLEMENTED, "");
    }
  };
  template <class BaseClass>
  class WithGenericMethod_myUnaryCall : public BaseClass {
   private:
    void BaseClassMustBeDerivedFromService(const Service* /*service*/) {}
   public:
    WithGenericMethod_myUnaryCall() {
      ::grpc::Service::MarkMethodGeneric(1);
    }
    ~WithGenericMethod_myUnaryCall() override {
      BaseClassMustBeDerivedFromService(this);
    }
    // disable synchronous version of this method
    ::grpc::Status myUnaryCall(::grpc::ServerContext* /*context*/, const ::com_hazelcast_platform_demos_banking_cva::InputMessage* /*request*/, ::com_hazelcast_platform_demos_banking_cva::OutputMessage* /*response*/) override {
      abort();
      return ::grpc::Status(::grpc::StatusCode::UNIMPLEMENTED, "");
    }
  };
  template <class BaseClass>
  class WithRawMethod_streamingCall : public BaseClass {
   private:
    void BaseClassMustBeDerivedFromService(const Service* /*service*/) {}
   public:
    WithRawMethod_streamingCall() {
      ::grpc::Service::MarkMethodRaw(0);
    }
    ~WithRawMethod_streamingCall() override {
      BaseClassMustBeDerivedFromService(this);
    }
    // disable synchronous version of this method
    ::grpc::Status streamingCall(::grpc::ServerContext* /*context*/, ::grpc::ServerReaderWriter< ::com_hazelcast_platform_demos_banking_cva::OutputMessage, ::com_hazelcast_platform_demos_banking_cva::InputMessage>* /*stream*/)  override {
      abort();
      return ::grpc::Status(::grpc::StatusCode::UNIMPLEMENTED, "");
    }
    void RequeststreamingCall(::grpc::ServerContext* context, ::grpc::ServerAsyncReaderWriter< ::grpc::ByteBuffer, ::grpc::ByteBuffer>* stream, ::grpc::CompletionQueue* new_call_cq, ::grpc::ServerCompletionQueue* notification_cq, void *tag) {
      ::grpc::Service::RequestAsyncBidiStreaming(0, context, stream, new_call_cq, notification_cq, tag);
    }
  };
  template <class BaseClass>
  class WithRawMethod_myUnaryCall : public BaseClass {
   private:
    void BaseClassMustBeDerivedFromService(const Service* /*service*/) {}
   public:
    WithRawMethod_myUnaryCall() {
      ::grpc::Service::MarkMethodRaw(1);
    }
    ~WithRawMethod_myUnaryCall() override {
      BaseClassMustBeDerivedFromService(this);
    }
    // disable synchronous version of this method
    ::grpc::Status myUnaryCall(::grpc::ServerContext* /*context*/, const ::com_hazelcast_platform_demos_banking_cva::InputMessage* /*request*/, ::com_hazelcast_platform_demos_banking_cva::OutputMessage* /*response*/) override {
      abort();
      return ::grpc::Status(::grpc::StatusCode::UNIMPLEMENTED, "");
    }
    void RequestmyUnaryCall(::grpc::ServerContext* context, ::grpc::ByteBuffer* request, ::grpc::ServerAsyncResponseWriter< ::grpc::ByteBuffer>* response, ::grpc::CompletionQueue* new_call_cq, ::grpc::ServerCompletionQueue* notification_cq, void *tag) {
      ::grpc::Service::RequestAsyncUnary(1, context, request, response, new_call_cq, notification_cq, tag);
    }
  };
  template <class BaseClass>
  class WithRawCallbackMethod_streamingCall : public BaseClass {
   private:
    void BaseClassMustBeDerivedFromService(const Service* /*service*/) {}
   public:
    WithRawCallbackMethod_streamingCall() {
      ::grpc::Service::MarkMethodRawCallback(0,
          new ::grpc::internal::CallbackBidiHandler< ::grpc::ByteBuffer, ::grpc::ByteBuffer>(
            [this](
                   ::grpc::CallbackServerContext* context) { return this->streamingCall(context); }));
    }
    ~WithRawCallbackMethod_streamingCall() override {
      BaseClassMustBeDerivedFromService(this);
    }
    // disable synchronous version of this method
    ::grpc::Status streamingCall(::grpc::ServerContext* /*context*/, ::grpc::ServerReaderWriter< ::com_hazelcast_platform_demos_banking_cva::OutputMessage, ::com_hazelcast_platform_demos_banking_cva::InputMessage>* /*stream*/)  override {
      abort();
      return ::grpc::Status(::grpc::StatusCode::UNIMPLEMENTED, "");
    }
    virtual ::grpc::ServerBidiReactor< ::grpc::ByteBuffer, ::grpc::ByteBuffer>* streamingCall(
      ::grpc::CallbackServerContext* /*context*/)
      { return nullptr; }
  };
  template <class BaseClass>
  class WithRawCallbackMethod_myUnaryCall : public BaseClass {
   private:
    void BaseClassMustBeDerivedFromService(const Service* /*service*/) {}
   public:
    WithRawCallbackMethod_myUnaryCall() {
      ::grpc::Service::MarkMethodRawCallback(1,
          new ::grpc::internal::CallbackUnaryHandler< ::grpc::ByteBuffer, ::grpc::ByteBuffer>(
            [this](
                   ::grpc::CallbackServerContext* context, const ::grpc::ByteBuffer* request, ::grpc::ByteBuffer* response) { return this->myUnaryCall(context, request, response); }));
    }
    ~WithRawCallbackMethod_myUnaryCall() override {
      BaseClassMustBeDerivedFromService(this);
    }
    // disable synchronous version of this method
    ::grpc::Status myUnaryCall(::grpc::ServerContext* /*context*/, const ::com_hazelcast_platform_demos_banking_cva::InputMessage* /*request*/, ::com_hazelcast_platform_demos_banking_cva::OutputMessage* /*response*/) override {
      abort();
      return ::grpc::Status(::grpc::StatusCode::UNIMPLEMENTED, "");
    }
    virtual ::grpc::ServerUnaryReactor* myUnaryCall(
      ::grpc::CallbackServerContext* /*context*/, const ::grpc::ByteBuffer* /*request*/, ::grpc::ByteBuffer* /*response*/)  { return nullptr; }
  };
  template <class BaseClass>
  class WithStreamedUnaryMethod_myUnaryCall : public BaseClass {
   private:
    void BaseClassMustBeDerivedFromService(const Service* /*service*/) {}
   public:
    WithStreamedUnaryMethod_myUnaryCall() {
      ::grpc::Service::MarkMethodStreamed(1,
        new ::grpc::internal::StreamedUnaryHandler<
          ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>(
            [this](::grpc::ServerContext* context,
                   ::grpc::ServerUnaryStreamer<
                     ::com_hazelcast_platform_demos_banking_cva::InputMessage, ::com_hazelcast_platform_demos_banking_cva::OutputMessage>* streamer) {
                       return this->StreamedmyUnaryCall(context,
                         streamer);
                  }));
    }
    ~WithStreamedUnaryMethod_myUnaryCall() override {
      BaseClassMustBeDerivedFromService(this);
    }
    // disable regular version of this method
    ::grpc::Status myUnaryCall(::grpc::ServerContext* /*context*/, const ::com_hazelcast_platform_demos_banking_cva::InputMessage* /*request*/, ::com_hazelcast_platform_demos_banking_cva::OutputMessage* /*response*/) override {
      abort();
      return ::grpc::Status(::grpc::StatusCode::UNIMPLEMENTED, "");
    }
    // replace default version of method with streamed unary
    virtual ::grpc::Status StreamedmyUnaryCall(::grpc::ServerContext* context, ::grpc::ServerUnaryStreamer< ::com_hazelcast_platform_demos_banking_cva::InputMessage,::com_hazelcast_platform_demos_banking_cva::OutputMessage>* server_unary_streamer) = 0;
  };
  typedef WithStreamedUnaryMethod_myUnaryCall<Service > StreamedUnaryService;
  typedef Service SplitStreamedService;
  typedef WithStreamedUnaryMethod_myUnaryCall<Service > StreamedService;
};

}  // namespace com_hazelcast_platform_demos_banking_cva


#endif  // GRPC_JetToCpp_2eproto__INCLUDED
